// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network.pneumatic;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.platform.Services;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityPneumaticStorageBase extends BlockEntityMachineBase
        implements IFluidHandlerMK2, IControlReceiver, IGUIProvider {

    @SyncField(units = 1L << 0)
    public final FluidTankNTM compair;

    private final Object2ObjectOpenCustomHashMap<ItemStack, Holding> index =
            new Object2ObjectOpenCustomHashMap<>(PneumaticNetwork.TYPE_AND_COMPONENTS);
    private long indexStamp = Long.MIN_VALUE;

    protected BlockEntityPneumaticStorageBase(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
        this.compair = new FluidTankNTM(NTMFluids.AIR, 4_000).withPressure(1);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {

        if (data.contains("pressure")) {
            int pressure = this.compair.getPressure() + 1;
            if (pressure > 5) pressure = 1;
            this.compair.setTankType(NTMFluids.AIR);
            this.compair.withPressure(pressure);
        }
    }

    @Override
    public void tickServer() {

        if (this.compair.getFill() > 0) {

            int consumption = this.compair.getFill() * 9 / this.compair.getMaxFill() + 1;
            this.compair.setFill(Math.max(this.compair.getFill() - consumption, 0));
        }

        this.networkPackNT(15);
    }

    public boolean isAvailable() {
        return !this.isRemoved() && this.compair.getFill() > 0;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(this.compair::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.compair.serialize(output.child("tank"));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return Services.PLATFORM.canFitInsideContainerItems(stack);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != compair.getPressure() || !compair.accepts(type)) return 0L;
        return (long) compair.getMaxFill() - compair.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != compair.getPressure() || !compair.accepts(type)) return amount;
        int accepted = compair.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public int[] getReceivingPressureRange(Fluid type) {
        return new int[] {compair.getPressure(), compair.getPressure()};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {compair};
    }

    public ItemStack getSlotAt(int index) {
        return this.getItem(index);
    }

    public abstract long getAmountAt(int index);

    public abstract long useUpItem(int index, long amount);

    public abstract long addItem(int index, long amount);

    public abstract long setupType(int index, ItemStack zeroStack, long amount);

    public abstract boolean allowTypeSetting();

    public boolean reaches(BlockPos viewer) {
        int range = BlockEntityPneumoTube.getRangeFromPressure(this.compair.getPressure());
        return worldPosition.distSqr(viewer) <= (double) range * range;
    }

    public Object2ObjectMap<ItemStack, Holding> index() {
        long now = level == null ? 0L : level.getGameTime();
        if (indexStamp == now) return index;
        indexStamp = now;
        index.clear();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack held = getSlotAt(slot);
            if (held.isEmpty()) continue;
            Holding holding = index.get(held);
            if (holding == null) {
                holding = new Holding(held.copyWithCount(1));
                index.put(holding.display, holding);
            }
            holding.total += getAmountAt(slot);
            holding.slots.add(slot);
        }
        return index;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        forgetIndex();
    }

    private void forgetIndex() {
        indexStamp = Long.MIN_VALUE;
    }

    public long take(ItemStack type, long amount) {
        Holding holding = index().get(type);
        if (holding == null) return amount;
        for (int slot : holding.slots) {
            if (!PneumaticNetwork.TYPE_AND_COMPONENTS.equals(getSlotAt(slot), type)) continue;
            amount = useUpItem(slot, amount);
            if (amount <= 0) break;
        }
        forgetIndex();
        return amount;
    }

    public long giveExisting(ItemStack stack, long amount) {
        Holding holding = index().get(stack);
        if (holding == null) return amount;
        for (int slot : holding.slots) {
            if (!PneumaticNetwork.TYPE_AND_COMPONENTS.equals(getSlotAt(slot), stack)) continue;
            amount = addItem(slot, amount);
            if (amount <= 0) break;
        }
        forgetIndex();
        return amount;
    }

    public long giveFresh(ItemStack stack, long amount) {
        if (!allowTypeSetting()) return amount;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getSlotAt(slot).isEmpty()) continue;
            amount = setupType(slot, stack, amount);
            if (amount <= 0) break;
        }
        forgetIndex();
        return amount;
    }

    public static final class Holding {

        public final ItemStack display;
        public final IntArrayList slots = new IntArrayList();
        public long total;

        Holding(ItemStack display) {
            this.display = display;
        }
    }

    public long syncUnitMask() {
        return 1L << 0;
    }

    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> this.compair.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.compair.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
