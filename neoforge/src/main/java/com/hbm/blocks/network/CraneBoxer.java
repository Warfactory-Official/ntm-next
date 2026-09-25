// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.network.BlockEntityCraneBoxer;
import com.hbm.util.InventoryUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CraneBoxer extends CraneBlockBase implements IEnterableBlock, ICapabilityBlock {

    public static final MapCodec<CraneBoxer> CODEC = simpleCodec(CraneBoxer::new);

    public CraneBoxer(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCraneBoxer(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CRANE_BOXER).items();
    }

    @Override
    public boolean canItemEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        return inputSide(level.getBlockState(pos)) == dir;
    }

    @Override
    public void onItemEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        if (dir == null) return;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCraneBoxer boxer)) return;
        store(level, pos, boxer, entity.getItemStack().copy(), dir);
    }

    @Override
    public boolean canPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        return true;
    }

    @Override
    public void onPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCraneBoxer boxer)) return;

        Direction accessed = getOutputSide(level, pos).getOpposite();
        for (ItemStack stack : entity.getItemStacks()) store(level, pos, boxer, stack, accessed);
    }

    private static void store(
            Level level,
            BlockPos pos,
            BlockEntityCraneBoxer boxer,
            ItemStack stack,
            Direction side) {
        ItemStack left = InventoryUtil.insertAt(level, pos, side, stack);

        if (!left.isEmpty()) {
            level.addFreshEntity(
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            left.copy()));
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
