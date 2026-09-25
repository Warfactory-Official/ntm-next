// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.CraneInserter;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuCraneInserter;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityCraneInserter extends BlockEntityCraneBase
        implements IGUIProvider, IControlReceiver, SyncUnitSchema {

    public static final int SLOT_COUNT = 21;

    private static final int[] ACCESS = accessAll();

    @SyncField(units = 1L << 0)
    public boolean destroyer = true;

    public BlockEntityCraneInserter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_INSERTER.get(), pos, state, SLOT_COUNT);
    }

    private static int[] accessAll() {
        int[] access = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) access[i] = i;
        return access;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneInserter");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public void tickServer() {
        if (!level.hasNeighborSignal(worldPosition)) push();
        networkPackNT(15);
    }

    private void push() {
        Direction output = getOutputSide();
        BlockPos targetPos = worldPosition.relative(output);
        if (!InventoryUtil.inventoryAt(level, targetPos, output.getOpposite())) return;

        Direction face = output.getOpposite();

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;

            ItemStack left = InventoryUtil.insertAt(level, targetPos, face, stack.copy());
            if (left.getCount() != stack.getCount()) {
                setItem(i, left);
                return;
            }
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;

            ItemStack left = InventoryUtil.insertAt(level, targetPos, face, stack.copyWithCount(1));
            if (left.isEmpty()) {
                removeItem(i, 1);
                return;
            }
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public int getComparatorPower() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCraneInserter(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        destroyer = input.getBooleanOr("destroyer", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("destroyer", destroyer);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < 400D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("destroyer")) this.destroyer = !this.destroyer;
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.destroyer);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.destroyer = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
