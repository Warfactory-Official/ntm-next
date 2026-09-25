// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockDecoToaster extends Block {

    public static final MapCodec<BlockDecoToaster> CODEC = simpleCodec(BlockDecoToaster::new);
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

    private static final VoxelShape EVEN = Shapes.box(0.25, 0, 0.375, 0.75, 0.325, 0.625);
    private static final VoxelShape ODD = Shapes.box(0.375, 0, 0.25, 0.625, 0.325, 0.75);

    public BlockDecoToaster(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection();
        int i = facing.get2DDataValue();
        return defaultBlockState().setValue(ROTATION, i);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(ROTATION) % 2 == 0 ? EVEN : ODD;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(
                ROTATION,
                rotation.rotate(Direction.from2DDataValue(state.getValue(ROTATION)))
                        .get2DDataValue());
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(
                mirror.getRotation(Direction.from2DDataValue(state.getValue(ROTATION))));
    }
}
