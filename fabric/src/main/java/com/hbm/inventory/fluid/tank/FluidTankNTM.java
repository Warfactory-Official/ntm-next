/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.fluid.tank;

import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.capability.FluidContainerRows;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.*;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import com.hbm.packet.codec.PacketBuffers;
import com.hbm.platform.Caps;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class FluidTankNTM implements SyncSource {

    private static final Random LOADER_RANDOM = new Random();

    private @Nullable Fluid type;
    private @Nullable Fluid pin;
    private @Nullable Fluid held;
    private int fill;
    private int capacity;
    private int pressure;
    private DataComponentPatch components;

    {
        hbm$syncSet$FluidTankNTM$components(DataComponentPatch.EMPTY);
    }

    private boolean storesComponents;
    private transient @Nullable BlockEntity owner;

    public FluidTankNTM(int capacity) {
        this.hbm$syncSet$FluidTankNTM$capacity(capacity);
    }

    public FluidTankNTM(@Nullable Fluid type, int capacity) {
        this.hbm$syncSet$FluidTankNTM$capacity(capacity);
        if (type != null && type != Fluids.EMPTY) this.hbm$syncSet$FluidTankNTM$type(type);
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

    public FluidTankNTM storesComponents() {
        storesComponents = true;
        return this;
    }

    public DataComponentPatch getComponents() {
        return components;
    }

    public void bindOwner(BlockEntity owner) {
        assert this.owner == null || this.owner == owner;
        this.owner = owner;
        scheduleConnections();
    }

    private void scheduleConnections() {
        if (owner != null) FluidPipeBlock.scheduleConnections(owner);
    }

    protected void declaredFluidChanged(@Nullable Fluid previous) {}

    public void setTankType(@Nullable Fluid fluid) {
        if (fluid == type) return;
        Fluid kind = kindOf(fluid);
        if (kind == type) return;
        Fluid previous = getDeclaredFluid();
        hbm$syncSet$FluidTankNTM$type(kind);
        hbm$syncSet$FluidTankNTM$pin(null);
        hbm$syncSet$FluidTankNTM$held(null);
        hbm$syncSet$FluidTankNTM$components(DataComponentPatch.EMPTY);
        hbm$syncSet$FluidTankNTM$fill(0);
        declaredFluidChanged(previous);
        scheduleConnections();
    }

    public void setTankTypeByIdentifier(@Nullable Fluid fluid) {
        Fluid previous = getDeclaredFluid();
        Fluid previousType = type;
        Fluid previousPin = pin;
        Fluid exact = (fluid == null || fluid == Fluids.EMPTY) ? null : fluid;
        Fluid kind = kindOf(exact);
        if (kind != type || fill > 0 && getFluid() != exact) {
            hbm$syncSet$FluidTankNTM$held(null);
            hbm$syncSet$FluidTankNTM$components(DataComponentPatch.EMPTY);
            hbm$syncSet$FluidTankNTM$fill(0);
        }
        hbm$syncSet$FluidTankNTM$type(kind);
        hbm$syncSet$FluidTankNTM$pin(exact);
        if (previous != getDeclaredFluid()) declaredFluidChanged(previous);
        if (previousType != type || previousPin != pin) scheduleConnections();
    }

    private static @Nullable Fluid kindOf(@Nullable Fluid fluid) {
        return (fluid == null || fluid == Fluids.EMPTY) ? null : NTMFluidProperties.kindOf(fluid);
    }

    public boolean accepts(@Nullable Fluid fluid) {
        return accepts(fluid, DataComponentPatch.EMPTY);
    }

    public boolean accepts(@Nullable Fluid fluid, DataComponentPatch patch) {
        if (fluid == null || fluid == Fluids.EMPTY || type == null) return false;
        if (!patch.isEmpty() && !storesComponents) return false;
        if (fill > 0) return fluid == getFluid() && patch.equals(components);
        if (pin != null) return fluid == pin;
        return fluid == type || NTMFluidProperties.kindOf(fluid) == type;
    }

    public boolean provides(@Nullable Fluid fluid) {
        return provides(fluid, DataComponentPatch.EMPTY);
    }

    public boolean provides(@Nullable Fluid fluid, DataComponentPatch patch) {
        return fluid != null && type != null && fluid == getFluid() && patch.equals(components);
    }

    public boolean joins(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY || type == null) return false;
        return pin != null
                ? fluid == pin
                : fluid == type || NTMFluidProperties.kindOf(fluid) == type;
    }

    public int receive(Fluid fluid, int amount) {
        return receive(fluid, DataComponentPatch.EMPTY, amount);
    }

    public int receive(Fluid fluid, DataComponentPatch patch, int amount) {
        assert patch.isEmpty() || storesComponents;
        int added = Math.min(amount, capacity - fill);
        if (added <= 0) return 0;
        if (fill == 0) {
            hbm$syncSet$FluidTankNTM$held(fluid == type ? null : fluid);
            hbm$syncSet$FluidTankNTM$components(patch);
        }
        hbm$syncSet$FluidTankNTM$fill((int) (fill + (added)));
        return added;
    }

    public FluidTankNTM conform(FluidStackNTM stack) {
        setTankType(stack.type());
        return withPressure(stack.pressure());
    }

    public FluidTankNTM withPressure(int pressure) {
        if (this.pressure != pressure) setFill(0);
        this.hbm$syncSet$FluidTankNTM$pressure(pressure);
        return this;
    }

    public int getPressure() {
        return pressure;
    }

    public int getFill() {
        return fill;
    }

    public void setFill(int amount) {
        this.hbm$syncSet$FluidTankNTM$fill(Math.clamp(amount, 0, capacity));
        if (fill == 0) emptied();
    }

    public void setFill(long amount) {
        this.hbm$syncSet$FluidTankNTM$fill((int) Math.clamp(amount, 0L, capacity));
        if (fill == 0) emptied();
    }

    private void emptied() {
        hbm$syncSet$FluidTankNTM$held(null);
        hbm$syncSet$FluidTankNTM$components(DataComponentPatch.EMPTY);
    }

    public int getMaxFill() {
        return capacity;
    }

    public void resetTank() {
        Fluid previous = getDeclaredFluid();
        hbm$syncSet$FluidTankNTM$type(null);
        hbm$syncSet$FluidTankNTM$pin(null);
        hbm$syncSet$FluidTankNTM$held(null);
        hbm$syncSet$FluidTankNTM$components(DataComponentPatch.EMPTY);
        hbm$syncSet$FluidTankNTM$fill(0);
        hbm$syncSet$FluidTankNTM$pressure(0);
        if (previous != null) {
            declaredFluidChanged(previous);
            scheduleConnections();
        }
    }

    public int changeTankSize(int size) {
        this.hbm$syncSet$FluidTankNTM$capacity(size);
        if (fill > capacity) {
            int diff = fill - capacity;
            hbm$syncSet$FluidTankNTM$fill(capacity);
            return diff;
        }
        return 0;
    }

    public int fill(@Nullable Fluid incoming, int amount, boolean doFill) {
        return fill(incoming, DataComponentPatch.EMPTY, amount, doFill);
    }

    public int fill(
            @Nullable Fluid incoming, DataComponentPatch patch, int amount, boolean doFill) {
        if (incoming == null || incoming == Fluids.EMPTY || amount <= 0) return 0;
        if (!patch.isEmpty() && !storesComponents) return 0;
        if (type == null) {
            int toTransfer = Math.min(capacity, amount);
            if (doFill) {
                hbm$syncSet$FluidTankNTM$type(NTMFluidProperties.kindOf(incoming));
                hbm$syncSet$FluidTankNTM$held(incoming == type ? null : incoming);
                hbm$syncSet$FluidTankNTM$components(patch);
                hbm$syncSet$FluidTankNTM$fill(toTransfer);
                declaredFluidChanged(null);
                scheduleConnections();
            }
            return toTransfer;
        }
        if (!accepts(incoming, patch)) return 0;
        int toTransfer = Math.min(capacity - fill, amount);
        return doFill ? receive(incoming, patch, toTransfer) : toTransfer;
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
        if (!components.isEmpty()) out.store("components", DataComponentPatch.CODEC, components);
    }

    public void deserialize(ValueInput in) {
        Fluid previousType = type;
        Fluid previousPin = pin;
        hbm$syncSet$FluidTankNTM$fill(in.getIntOr("fill", 0));
        hbm$syncSet$FluidTankNTM$capacity(in.getIntOr("capacity", capacity));
        hbm$syncSet$FluidTankNTM$pressure(in.getIntOr("pressure", 0));
        hbm$syncSet$FluidTankNTM$type(resolveFluid(in.getStringOr("type", "")));
        hbm$syncSet$FluidTankNTM$pin(resolveFluid(in.getStringOr("pin", "")));
        hbm$syncSet$FluidTankNTM$fill(Math.clamp(fill, 0, capacity));
        String heldId = in.getStringOr("held", "");
        hbm$syncSet$FluidTankNTM$held(resolveFluid(heldId));
        hbm$syncSet$FluidTankNTM$components(
                in.read("components", DataComponentPatch.CODEC).orElse(DataComponentPatch.EMPTY));
        if (!heldId.isEmpty() && held == null) hbm$syncSet$FluidTankNTM$fill(0);
        if (fill == 0) emptied();
        if (previousType != type || previousPin != pin) scheduleConnections();
    }

    public void packetSerialize(ByteBuf buf) {
        packetSerialize(buf, fill);
    }

    public void packetSerialize(ByteBuf buf, int snapshotFill) {
        buf.writeInt(snapshotFill);
        buf.writeInt(capacity);
        buf.writeInt(pressure);
        Fluid fluid = getFluid();
        int id = fluid == null ? -1 : BuiltInRegistries.FLUID.getId(fluid);
        if (components.isEmpty() || id < 0) {
            buf.writeInt(id);
        } else {
            buf.writeInt(-2 - id);
            DataComponentPatch.STREAM_CODEC.encode(PacketBuffers.registry(buf, true), components);
        }
    }

    public void packetDeserialize(ByteBuf buf) {
        hbm$syncSet$FluidTankNTM$fill(buf.readInt());
        hbm$syncSet$FluidTankNTM$capacity(buf.readInt());
        hbm$syncSet$FluidTankNTM$pressure(buf.readInt());
        int id = buf.readInt();
        boolean patched = id < -1;
        if (patched) id = -2 - id;
        Fluid decoded = id < 0 ? null : BuiltInRegistries.FLUID.byId(id);
        Fluid fluid = decoded == Fluids.EMPTY ? null : decoded;
        hbm$syncSet$FluidTankNTM$components(
                patched
                        ? DataComponentPatch.STREAM_CODEC.decode(PacketBuffers.registry(buf, false))
                        : DataComponentPatch.EMPTY);
        hbm$syncSet$FluidTankNTM$fill(Math.clamp(fill, 0, capacity));
        hbm$syncSet$FluidTankNTM$type(kindOf(fluid));
        boolean foreign = fluid != type;
        hbm$syncSet$FluidTankNTM$held(foreign && fill > 0 ? fluid : null);
        hbm$syncSet$FluidTankNTM$pin(foreign && fill == 0 ? fluid : null);
    }

    private void serveFillableMods(ItemStack armor, boolean drainMods) {
        if (type == null || !components.isEmpty() || !ArmorModHandler.hasMods(armor)) return;
        var mods = ArmorModHandler.mods(armor);
        for (int socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
            if (!(mods.item(socket) instanceof FluidContainerItem container)
                    || !container.machineFillable()) continue;
            ItemStack mod = mods.copy(socket);
            if (drainMods)
                hbm$syncSet$FluidTankNTM$fill(
                        (int) (fill + (container.drain(mod, type, capacity - fill, pressure))));
            else
                hbm$syncSet$FluidTankNTM$fill(
                        (int) (fill - (container.fill(mod, type, fill, pressure))));
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

        if (stack.getItem() instanceof FluidContainerInfiniteItem item) {
            Fluid given = item.fluid() == null ? getFluid() : item.fluid();
            if (type != null
                    && accepts(given)
                    && item.operates(stack)
                    && (item.chance() <= 1 || LOADER_RANDOM.nextInt(item.chance()) == 0)) {
                receive(given, item.amount());
            }
        } else if (stack.getItem() instanceof FluidContainerItem container
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

        if (stack.getItem() instanceof FluidContainerInfiniteItem item) {
            if (item.allowPressure(pressure)
                    && (item.fluid() == null || item.fluid() == getFluid())
                    && item.operates(stack)
                    && (item.chance() <= 1 || LOADER_RANDOM.nextInt(item.chance()) == 0)) {
                setFill(Math.max(fill - item.amount(), 0));
            }
        } else if (pressure == 0) {
            serveFillableMods(stack, false);
            if (stack.getItem() instanceof FluidContainerItem container
                    && container.machineFillable()) {
                if (type != null && components.isEmpty())
                    setFill(fill - container.fill(stack, getFluid(), fill, pressure));
            } else {
                fillContainer(slots, in, out);
            }
        }
        return fill < prev;
    }

    public static boolean isFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof FluidContainerInfiniteItem) return true;
        return Caps.findFluidStore(stack) != null;
    }

    public static long containerContent(ItemStack stack, @Nullable Fluid type) {
        if (type == null || stack.isEmpty() || !convertible(stack)) return 0L;
        if (FluidContainerRows.answers(stack)) {
            FluidContainerRows.Row row = FluidContainerRows.full(stack);
            return row != null && row.fluid().get() == type ? row.amount().getAsInt() : 0L;
        }
        Storage<FluidVariant> store = Caps.findFluidStore(stack);
        return store == null ? 0L : Caps.extractable(store, type);
    }

    public static long containerRoom(ItemStack stack, @Nullable Fluid type) {
        if (type == null || stack.isEmpty() || !convertible(stack)) return 0L;
        if (FluidContainerRows.answers(stack)) return FluidContainerRows.fillAmount(stack, type);
        Storage<FluidVariant> store = Caps.findFluidStore(stack);
        return store == null ? 0L : Caps.insertable(store, type);
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
        return !(stack.getItem() instanceof FluidContainerItem container)
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
        Storage<FluidVariant> store = Caps.findFluidStore(scratch, 0);
        if (store == null) return;

        Fluid incoming = null;
        DataComponentPatch patch = DataComponentPatch.EMPTY;
        long offered = 0L;
        Fluid[] equivalents = NTMFluidProperties.equivalents(type);
        List<DataComponentPatch> patches = storesComponents ? new ArrayList<>() : List.of();
        search:
        for (int i = -1; i < equivalents.length; i++) {
            Fluid candidate = i < 0 ? type : equivalents[i];
            if (accepts(candidate)) {
                offered = Caps.extractable(store, candidate);
                if (offered > 0) {
                    incoming = candidate;
                    break;
                }
            }
            if (!storesComponents) continue;
            patches.clear();
            Caps.forEachComponents(store, candidate, patches::add);
            for (DataComponentPatch candidatePatch : patches) {
                if (!accepts(candidate, candidatePatch)) continue;
                offered = Caps.extractable(store, candidate, candidatePatch);
                if (offered > 0) {
                    incoming = candidate;
                    patch = candidatePatch;
                    break search;
                }
            }
        }
        if (incoming == null) return;
        long room = (long) capacity - fill;
        long drawn = Caps.extract(store, incoming, patch, Math.min(offered, room));
        if (drawn <= 0) return;

        ItemStack result = scratch.getItem(0);
        if (!changed(probe, result)) return;
        if (Caps.extractable(store, incoming, patch) <= 0) {
            if (!ejectFits(slots, out, result)) return;
            receive(incoming, patch, (int) drawn);
            eject(slots, in, out, result);
            return;
        }
        if (held.getCount() != 1) return;
        receive(incoming, patch, (int) drawn);
        slots.set(in, result);
    }

    private void fillContainer(NonNullList<ItemStack> slots, int in, int out) {
        if (type == null) return;
        ItemStack held = slots.get(in);
        if (!convertible(held)) return;
        Fluid content = getFluid();
        if (FluidContainerRows.answers(held)) {
            if (!components.isEmpty()) return;
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
        Storage<FluidVariant> store = Caps.findFluidStore(scratch, 0);
        if (store == null) return;

        Fluid offered = content;
        long room = Caps.insertable(store, content, components);
        if (room <= 0 && content == type && components.isEmpty()) {
            for (Fluid placed : NTMFluidProperties.equivalents(type)) {
                if (!(placed instanceof ClassicFluid classic) || classic.spec().kind() == null)
                    break;
                room = Caps.insertable(store, placed);
                if (room > 0) {
                    offered = placed;
                    break;
                }
            }
        }
        if (room <= 0) return;
        long given = Caps.insert(store, offered, components, Math.min(room, fill));
        if (given <= 0) return;

        ItemStack result = scratch.getItem(0);
        if (!changed(probe, result)) return;
        if (Caps.insertable(store, offered, components) <= 0) {
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

    public final void hbm$syncSet$FluidTankNTM$type(Fluid next) {
        if (this.type != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.type);
            this.type = next;
            if (bound) bindSyncValue(next, 3, 0L);
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$FluidTankNTM$pin(Fluid next) {
        if (this.pin != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.pin);
            this.pin = next;
            if (bound) bindSyncValue(next, 3, 0L);
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$FluidTankNTM$held(Fluid next) {
        if (this.held != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.held);
            this.held = next;
            if (bound) bindSyncValue(next, 3, 0L);
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$FluidTankNTM$fill(int next) {
        if (this.fill != next) {
            this.fill = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$FluidTankNTM$capacity(int next) {
        if (this.capacity != next) {
            this.capacity = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$FluidTankNTM$pressure(int next) {
        if (this.pressure != next) {
            this.pressure = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$FluidTankNTM$components(DataComponentPatch next) {
        if (this.components != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.components);
            this.components = next;
            if (bound) bindSyncValue(next, 3, 0L);
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FluidTankNTM.class, "type", 3, 0L);
        com.hbm.packet.SyncBindings.field(FluidTankNTM.class, "pin", 3, 0L);
        com.hbm.packet.SyncBindings.field(FluidTankNTM.class, "held", 3, 0L);
        com.hbm.packet.SyncBindings.field(FluidTankNTM.class, "components", 3, 0L);
    }

    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
