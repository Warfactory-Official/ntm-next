// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.network.BlockEntityCraneInserter;
import com.hbm.util.InventoryUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CraneInserter extends CraneBlockBase implements IEnterableBlock, ICapabilityBlock {

    public static final MapCodec<CraneInserter> CODEC = simpleCodec(CraneInserter::new);

    public CraneInserter(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCraneInserter(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CRANE_INSERTER).items();
    }

    @Override
    public boolean canItemEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        return inputSide(level.getBlockState(pos)) == dir;
    }

    @Override
    public void onItemEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        ItemStack carried = entity.getItemStack();
        if (carried.isEmpty()) return;
        insert(level, pos, carried.copy());
    }

    @Override
    public boolean canPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        return true;
    }

    @Override
    public void onPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        if (entity.getItemStacks().isEmpty()) return;

        Direction output = getOutputSide(level, pos);

        if (!level.hasNeighborSignal(pos)) {
            for (ItemStack stack : entity.getItemStacks()) {
                InventoryUtil.insertAt(level, pos.relative(output), output.getOpposite(), stack);
            }
        }

        for (ItemStack stack : entity.getItemStacks()) buffer(level, pos, output, stack);
    }

    private void insert(Level level, BlockPos pos, ItemStack toAdd) {
        Direction output = getOutputSide(level, pos);

        if (!level.hasNeighborSignal(pos)) {
            InventoryUtil.insertAt(level, pos.relative(output), output.getOpposite(), toAdd);
        }

        buffer(level, pos, output, toAdd);
    }

    private void buffer(Level level, BlockPos pos, Direction output, ItemStack toAdd) {
        if (toAdd.isEmpty()) return;

        BlockEntityCraneInserter inserter =
                level.getBlockEntity(pos) instanceof BlockEntityCraneInserter be ? be : null;
        if (inserter == null) return;

        InventoryUtil.insertAt(level, pos, output.getOpposite(), toAdd);

        if (!toAdd.isEmpty() && !inserter.destroyer) {
            level.addFreshEntity(
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            toAdd.copy()));
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof Container c
                ? AbstractContainerMenu.getRedstoneSignalFromContainer(c)
                : 0;
    }
}
