// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineRTG;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.util.RTGUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineRTG extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, IGUIProvider, SyncUnitSchema {

    public static final int SLOT_COUNT = 15;
    public static final int[] SLOT_IO = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
    public static final long MAX_POWER = 100_000L;

    public static final int MAX_HEAT = RTGUtil.RTG_DECAY ? 600 : 200;
    public static final int POWER_PER_HEAT = 5;

    @SyncField(units = 1L << 0)
    public long power;

    @ContainerSync public int heat;

    public BlockEntityMachineRTG(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_RTG.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        long prevPower = power;
        int prevHeat = heat;

        heat = Math.min(RTGUtil.updateRTGs(inventory, SLOT_IO), MAX_HEAT);
        power = Math.min(MAX_POWER, power + (long) heat * POWER_PER_HEAT);

        if (power != prevPower || heat != prevHeat) setChanged();
        networkPackNT(50);
    }

    public boolean hasHeat() {
        return RTGUtil.hasHeat(inventory, SLOT_IO);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemRTGPellet;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOT_IO;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rtg");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineRTG(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.getInt("heat").ifPresent(v -> heat = v);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("heat", heat);
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            default -> throw new IllegalArgumentException();
        }
    }
}
