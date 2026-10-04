// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntitySoyuzRenderAnchor;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class LaunchpadSoyuzAnchorCell extends BlockMultiblockCell
        implements EntityBlock, ITickingBlock {
    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 1, 3);

    public LaunchpadSoyuzAnchorCell(Properties properties) {
        super(properties);
        registerDefaultState(
                defaultBlockState()
                        .setValue(BlockMultiblockCore.FACING, Direction.NORTH)
                        .setValue(SEGMENT, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockMultiblockCore.FACING, SEGMENT);
    }

    public static BlockPos core(BlockPos anchor, BlockState state) {
        int segment = state.getValue(SEGMENT);
        Direction facing = state.getValue(BlockMultiblockCore.FACING);
        return anchor.offset(
                -LaunchpadSoyuz.anchorX(segment, facing),
                0,
                -LaunchpadSoyuz.anchorZ(segment, facing));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntitySoyuzRenderAnchor(pos, state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(
                BlockMultiblockCore.FACING,
                rotation.rotate(state.getValue(BlockMultiblockCore.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockMultiblockCore.FACING)));
    }
}
