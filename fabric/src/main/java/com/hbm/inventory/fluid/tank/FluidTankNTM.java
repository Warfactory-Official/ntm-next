// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.tank;

import com.hbm.capability.FluidContainerRows;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.*;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.platform.IFluidHandlerView;
import com.hbm.platform.Services;
import io.netty.buffer.ByteBuf;
import java.util.Random;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class FluidTankNTM implements SyncSource {

    private static final Random LOADER_RANDOM = new Random();

    @SyncField private @Nullable Fluid type;

    @SyncField private @Nullable Fluid pin;

    @SyncField private @Nullable Fluid held;
    @SyncField private int fill;
    @SyncField private int capacity;
    @SyncField private int pressure;

    public FluidTankNTM(int capacity) {
        this.capacity = capacity;
    }

    public FluidTankNTM(@Nullable Fluid type, int capacity) {
        this.capacity = capacity;
        if (type != null && type != Fluids.EMPTY) this.type = type;
    }

    private static @Nullable Fluid resolveFluid(String id) {
        if (id.isEmpty()) return null;
        Fluid f = BuiltInRegistries.FLUID.getValue(Identifier.parse(id));
        return f == Fluids.EMPTY ? null : f;
    }

    private static boolean ejectFits(NonNullList<ItemStack> slots, int out, ItemStack result) {
        ItemStack existing = slots.get(out);
        if (existing.isEmpty() || result.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(existing, result)
                && existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    private static void eject(NonNullList<ItemStack> slots, int in, int out, ItemStack result) {
        ItemStack source = slots.get(in);
        source.shrink(1);
        if (source.isEmpty()) slots.set(in, ItemStack.EMPTY);
        ItemStack existing = slots.get(out);
        if (existing.isEmpty()) {
            slots.set(out, result);
        } else {
            existing.grow(result.getCount());
        }
    }

    public @Nullable Fluid getTankType() {
        return type;
    }

    public @Nullable Fluid getFluid() {
        return held != null ? held : pin != null ? pin : type;
    }

    public @Nullable Fluid getDeclaredFluid() {
        return pin != null ? pin : type;
    }

    public boolean isStrict() {
        return pin != null;
    }

    public void setTankType(@Nullable Fluid fluid) {
        if (fluid == type) return;
        Fluid kind = kindOf(fluid);
        if (kind == type) return;
        type = kind;
        pin = null;
        held = null;
        fill = 0;
    }

    public void setTankTypeByIdentifier(@Nullable Fluid fluid) {
        Fluid exact = (fluid == null || fluid == Fluids.EMPTY) ? null : fluid;
        Fluid kind = kindOf(exact);
        if (kind != type || fill > 0 && getFluid() != exact) {
            held = null;
            fill = 0;
        }
        type = kind;
        pin = exact;
    }

    private static @Nullable Fluid kindOf(@Nullable Fluid fluid) {
        return (fluid == null || fluid == Fluids.EMPTY) ? null : NTMFluidProperties.kindOf(fluid);
    }

    public boolean accepts(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY || type == null) return false;
        if (fill > 0) return fluid == getFluid();
        if (pin != null) return fluid == pin;
        return fluid == type || NTMFluidProperties.kindOf(fluid) == type;
    }

    public boolean provides(@Nullable Fluid fluid) {
        return fluid != null && type != null && fluid == getFluid();
    }

    public int receive(Fluid fluid, int amount) {
        int added = Math.min(amount, capacity - fill);
        if (added <= 0) return 0;
        if (fill == 0) held = fluid == type ? null : fluid;
        fill += added;
        return added;
    }

    public FluidTankNTM conform(FluidStackNTM stack) {
        setTankType(stack.type());
        return withPressure(stack.pressure());
    }

    public FluidTankNTM withPressure(int pressure) {
        if (this.pressure != pressure) setFill(0);
        this.pressure = pressure;
        return this;
    }

    public int getPressure() {
        return pressure;
    }

    public int getFill() {
        return fill;
    }

    public void setFill(int amount) {
        this.fill = Math.clamp(amount, 0, capacity);
        if (fill == 0) held = null;
    }

    public void setFill(long amount) {
        this.fill = (int) Math.clamp(amount, 0L, capacity);
        if (fill == 0) held = null;
    }

    public int getMaxFill() {
        return capacity;
    }

    public void resetTank() {
        type = null;
        pin = null;
        held = null;
        fill = 0;
        pressure = 0;
    }

    public int changeTankSize(int size) {
        this.capacity = size;
        if (fill > capacity) {
            int diff = fill - capacity;
            fill = capacity;
            return diff;
        }
        return 0;
    }

    public int fill(@Nullable Fluid incoming, int amount, boolean doFill) {
        if (incoming == null || incoming == Fluids.EMPTY || amount <= 0) return 0;
        if (type == null) {
            int toTransfer = Math.min(capacity, amount);
            if (doFill) {
                type = NTMFluidProperties.kindOf(incoming);
                held = incoming == type ? null : incoming;
                fill = toTransfer;
            }
            return toTransfer;
        }
        if (!accepts(incoming)) return 0;
        int toTransfer = Math.min(capacity - fill, amount);
        return doFill ? receive(incoming, toTransfer) : toTransfer;
    }

    public int drain(int amount, boolean doDrain) {
        if (amount <= 0) return 0;
        int drained = Math.min(fill, amount);
        if (doDrain) setFill(fill - drained);
        return drained;
    }

    public void serialize(ValueOutput out) {
        out.putInt("fill", fill);
        out.putInt("capacity", capacity);
        out.putInt("pressure", pressure);
        if (type != null) out.putString("type", BuiltInRegistries.FLUID.getKey(type).toString());
        if (pin != null) out.putString("pin", BuiltInRegistries.FLUID.getKey(pin).toString());
        if (held != null) out.putString("held", BuiltInRegistries.FLUID.getKey(held).toString());
    }

    public void deserialize(ValueInput in) {
        fill = in.getIntOr("fill", 0);
        capacity = in.getIntOr("capacity", capacity);
        pressure = in.getIntOr("pressure", 0);
        type = resolveFluid(in.getStringOr("type", ""));
        pin = resolveFluid(in.getStringOr("pin", ""));
        fill = Math.clamp(fill, 0, capacity);
        String heldId = in.getStringOr("held", "");
        held = resolveFluid(heldId);

        if (!heldId.isEmpty() && held == null) fill = 0;
        if (fill == 0) held = null;
    }

    public void packetSerialize(ByteBuf buf) {
        packetSerialize(buf, fill);
    }

    public void packetSerialize(ByteBuf buf, int snapshotFill) {
        buf.writeInt(snapshotFill);
        buf.writeInt(capacity);
        buf.writeInt(pressure);
        Fluid fluid = getFluid();
        buf.writeInt(fluid == null ? -1 : BuiltInRegistries.FLUID.getId(fluid));
    }

    public void packetDeserialize(ByteBuf buf) {
        fill = buf.readInt();
        capacity = buf.readInt();
        pressure = buf.readInt();
        int id = buf.readInt();
        Fluid decoded = id < 0 ? null : BuiltInRegistries.FLUID.byId(id);
        Fluid fluid = decoded == Fluids.EMPTY ? null : decoded;
        fill = Math.clamp(fill, 0, capacity);
        type = kindOf(fluid);
        boolean foreign = fluid != type;
        held = foreign && fill > 0 ? fluid : null;
        pin = foreign && fill == 0 ? fluid : null;
    }

    private void serveFillableMods(ItemStack armor, boolean drainMods) {
        if (type == null || !ArmorModHandler.hasMods(armor)) return;
        for (ItemStack mod : ArmorModHandler.pryMods(armor)) {
            if (!(mod.getItem() instanceof IFluidContainerItem container)
                    || !container.machineFillable()) continue;
            if (drainMods) fill += container.drain(mod, type, capacity - fill, pressure);
            else fill -= container.fill(mod, type, fill, pressure);

            ArmorModHandler.applyMod(armor, mod);
        }
    }

    public boolean loadTank(int in, int out, NonNullList<ItemStack> slots) {
        ItemStack stack = slots.get(in);
        if (stack.isEmpty()) return false;

        boolean isInfiniteBarrel = stack.getItem() == ModItems.FLUID_BARREL_INFINITE.get();
        if (!isInfiniteBarrel && pressure != 0) return false;
        int prev = fill;
        serveFillableMods(stack, true);

        if (stack.getItem() instanceof ItemFluidContainerInfinite item) {

            Fluid given = item.fluid() == null ? getFluid() : item.fluid();
            if (type != null
                    && accepts(given)
                    && (item.chance() <= 1 || LOADER_RANDOM.nextInt(item.chance()) == 0)) {
                receive(given, item.amount());
            }
        } else if (stack.getItem() instanceof IFluidContainerItem container
                && container.machineFillable()) {

            if (accepts(type))
                receive(type, container.drain(stack, type, capacity - fill, pressure));
        } else {
            emptyContainer(slots, in, out);
        }
        return fill > prev;
    }

    public boolean unloadTank(int in, int out, NonNullList<ItemStack> slots) {
        ItemStack stack = slots.get(in);
        if (stack.isEmpty()) return false;
        int prev = fill;

        if (stack.getItem() instanceof ItemFluidContainerInfinite item) {

            if (item.allowPressure(pressure)
                    && (item.fluid() == null || item.fluid() == getFluid())
                    && (item.chance() <= 1 || LOADER_RANDOM.nextInt(item.chance()) == 0)) {
                setFill(Math.max(fill - item.amount(), 0));
            }
        } else if (pressure == 0) {
            serveFillableMods(stack, false);
            if (stack.getItem() instanceof IFluidContainerItem container
                    && container.machineFillable()) {

                if (type != null) setFill(fill - container.fill(stack, getFluid(), fill, pressure));
            } else {
                fillContainer(slots, in, out);
            }
        }
        return fill < prev;
    }

    public static boolean isFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemFluidContainerInfinite) return true;
        return Services.CAPS.findFluidHandler(stack) != null;
    }

    public static long containerContent(ItemStack stack, @Nullable Fluid type) {
        if (type == null || stack.isEmpty() || !convertible(stack)) return 0L;
        if (FluidContainerRows.answers(stack)) {
            FluidContainerRows.Row row = FluidContainerRows.full(stack);
            return row != null && row.fluid().get() == type ? row.amount().getAsInt() : 0L;
        }
        IFluidHandlerView view = Services.CAPS.findFluidHandler(stack);
        return view == null ? 0L : view.extractable(type);
    }

    public static long containerRoom(ItemStack stack, @Nullable Fluid type) {
        if (type == null || stack.isEmpty() || !convertible(stack)) return 0L;
        if (FluidContainerRows.answers(stack)) return FluidContainerRows.fillAmount(stack, type);
        IFluidHandlerView view = Services.CAPS.findFluidHandler(stack);
        return view == null ? 0L : view.insertable(type);
    }

    public long containerContent(ItemStack stack) {
        if (type == null) return 0L;
        Fluid[] equivalents = NTMFluidProperties.equivalents(type);
        for (int i = -1; i < equivalents.length; i++) {
            Fluid candidate = i < 0 ? type : equivalents[i];
            if (!accepts(candidate)) continue;
            long content = containerContent(stack, candidate);
            if (content > 0) return content;
        }
        return 0L;
    }

    public long containerRoom(ItemStack stack) {
        return containerRoom(stack, getFluid());
    }

    private static boolean convertible(ItemStack stack) {
        return !(stack.getItem() instanceof IFluidContainerItem container)
                || container.machineConvertible();
    }

    private static boolean changed(ItemStack probe, ItemStack result) {
        return !ItemStack.matches(probe, result);
    }

    private void emptyContainer(NonNullList<ItemStack> slots, int in, int out) {
        if (type == null || pressure != 0) return;
        ItemStack held = slots.get(in);
        if (!convertible(held)) return;
        if (FluidContainerRows.answers(held)) {
            FluidContainerRows.Row row = FluidContainerRows.full(held);
            if (row == null || !accepts(row.fluid().get())) return;
            int amount = row.amount().getAsInt();
            ItemStack result = row.emptyStack();
            if ((long) fill + amount > capacity || !ejectFits(slots, out, result)) return;
            receive(row.fluid().get(), amount);
            eject(slots, in, out, result);
            return;
        }
        ItemStack probe = held.copyWithCount(1);
        SimpleContainer scratch = new SimpleContainer(probe.copy());
        IFluidHandlerView view = Services.CAPS.findFluidHandler(scratch, 0);
        if (view == null) return;

        Fluid incoming = null;
        long offered = 0L;
        Fluid[] equivalents = NTMFluidProperties.equivalents(type);
        for (int i = -1; i < equivalents.length; i++) {
            Fluid candidate = i < 0 ? type : equivalents[i];
            if (!accepts(candidate)) continue;
            offered = view.extractable(candidate);
            if (offered > 0) {
                incoming = candidate;
                break;
            }
        }
        if (incoming == null) return;
        long room = (long) capacity - fill;
        long drawn = view.extract(incoming, Math.min(offered, room));
        if (drawn <= 0) return;

        ItemStack result = scratch.getItem(0);
        if (!changed(probe, result)) return;

        if (view.extractable(incoming) <= 0) {
            if (!ejectFits(slots, out, result)) return;
            receive(incoming, (int) drawn);
            eject(slots, in, out, result);
            return;
        }

        if (held.getCount() != 1) return;
        receive(incoming, (int) drawn);
        slots.set(in, result);
    }

    private void fillContainer(NonNullList<ItemStack> slots, int in, int out) {
        if (type == null) return;
        ItemStack held = slots.get(in);
        if (!convertible(held)) return;
        Fluid content = getFluid();
        if (FluidContainerRows.answers(held)) {
            int amount = FluidContainerRows.fillAmount(held, content);
            if (amount <= 0 || fill < amount) return;
            ItemStack result = FluidContainerRows.filled(held, content);
            if (!ejectFits(slots, out, result)) return;
            setFill(fill - amount);
            eject(slots, in, out, result);
            return;
        }
        ItemStack probe = held.copyWithCount(1);
        SimpleContainer scratch = new SimpleContainer(probe.copy());
        IFluidHandlerView view = Services.CAPS.findFluidHandler(scratch, 0);
        if (view == null) return;

        long room = view.insertable(content);
        if (room <= 0) return;
        long given = view.insert(content, Math.min(room, fill));
        if (given <= 0) return;

        ItemStack result = scratch.getItem(0);
        if (!changed(probe, result)) return;

        if (view.insertable(content) <= 0) {
            if (!ejectFits(slots, out, result)) return;
            setFill(fill - (int) given);
            eject(slots, in, out, result);
            return;
        }
        if (held.getCount() != 1) return;
        setFill(fill - (int) given);
        slots.set(in, result);
    }

    public boolean setType(int in, int out, NonNullList<ItemStack> slots) {
        ItemStack stack = slots.get(in);
        if (!(stack.getItem() instanceof FluidIdentifierItem)) return false;
        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid primary = data.primary();
        Fluid target = (primary == null || primary == Fluids.EMPTY) ? null : primary;
        boolean unchanged = pin == target && type == kindOf(target);
        if (in == out) {
            if (unchanged) return false;
            setTankTypeByIdentifier(target);
            return true;
        }
        if (!slots.get(out).isEmpty() || unchanged) return false;
        setTankTypeByIdentifier(target);
        slots.set(out, stack.copy());
        slots.set(in, ItemStack.EMPTY);
        return true;
    }
}
