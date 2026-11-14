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

package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SandbagsBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public static final int SIDES = 16;

    private static final VoxelShape[] SHAPES = buildShapes();

    public SandbagsBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(WATERLOGGED, false));
    }

    public static BooleanProperty propertyFor(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException(dir.toString());
        };
    }

    public static int[] boxOf(int mask) {
        return new int[] {
            set(mask, Direction.WEST) ? 0 : 4,
            set(mask, Direction.NORTH) ? 0 : 4,
            set(mask, Direction.EAST) ? 16 : 12,
            set(mask, Direction.SOUTH) ? 16 : 12
        };
    }

    public static boolean set(int mask, Direction dir) {
        return (mask & (1 << dir.get2DDataValue())) != 0;
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[SIDES];
        for (int mask = 0; mask < SIDES; mask++) {
            int[] box = boxOf(mask);
            shapes[mask] = Shapes.box(box[0] / 16D, 0, box[1] / 16D, box[2] / 16D, 1, box[3] / 16D);
        }
        return shapes;
    }

    private boolean joins(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(this) || state.isSolidRender();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state =
                defaultBlockState()
                        .setValue(
                                WATERLOGGED,
                                ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            state =
                    state.setValue(
                            propertyFor(dir),
                            joins(ctx.getLevel(), ctx.getClickedPos().relative(dir)));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction dir,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED))
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (dir.getAxis().isVertical()) return state;
        return state.setValue(
                propertyFor(dir), neighbourState.is(this) || neighbourState.isSolidRender());
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (state.getValue(propertyFor(dir))) mask |= 1 << dir.get2DDataValue();
        }
        return SHAPES[mask];
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @java.lang.Override
    protected net.minecraft.world.level.block.state.BlockState rotate(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.Rotation rotation) {
        if (rotation == net.minecraft.world.level.block.Rotation.CLOCKWISE_90)
            return state.setValue(NORTH, state.getValue(WEST))
                    .setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST))
                    .setValue(WEST, state.getValue(SOUTH));
        if (rotation == net.minecraft.world.level.block.Rotation.CLOCKWISE_180)
            return state.setValue(NORTH, state.getValue(SOUTH))
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH))
                    .setValue(WEST, state.getValue(EAST));
        if (rotation == net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90)
            return state.setValue(NORTH, state.getValue(EAST))
                    .setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST))
                    .setValue(WEST, state.getValue(NORTH));
        return state;
    }

    @java.lang.Override
    protected net.minecraft.world.level.block.state.BlockState mirror(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.Mirror mirror) {
        if (mirror == net.minecraft.world.level.block.Mirror.LEFT_RIGHT)
            return state.setValue(NORTH, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(NORTH));
        if (mirror == net.minecraft.world.level.block.Mirror.FRONT_BACK)
            return state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
        return state;
    }
}
