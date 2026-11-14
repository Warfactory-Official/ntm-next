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

package com.hbm.blocks.network;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.CapabilityDependentShape;
import com.hbm.capability.PhysicalConnector;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CableBlock extends Block
        implements CapabilityDependentShape, SimpleWaterloggedBlock, PhysicalConnector {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final int OPEN_ALL = 0x3F;

    private static final VoxelShape[] SHAPES = buildShapes();

    public CableBlock(BlockBehaviour.Properties props) {
        super(CapabilityDependentShape.deferShapeToFullChunk(props));
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(UP, false)
                        .setValue(DOWN, false)
                        .setValue(WATERLOGGED, false));
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape core = Shapes.box(5 / 16D, 5 / 16D, 5 / 16D, 11 / 16D, 11 / 16D, 11 / 16D);
        VoxelShape[] arm = new VoxelShape[6];
        arm[Direction.DOWN.ordinal()] =
                Shapes.box(5 / 16D, 0, 5 / 16D, 11 / 16D, 5 / 16D, 11 / 16D);
        arm[Direction.UP.ordinal()] = Shapes.box(5 / 16D, 11 / 16D, 5 / 16D, 11 / 16D, 1, 11 / 16D);
        arm[Direction.NORTH.ordinal()] =
                Shapes.box(5 / 16D, 5 / 16D, 0, 11 / 16D, 11 / 16D, 5 / 16D);
        arm[Direction.SOUTH.ordinal()] =
                Shapes.box(5 / 16D, 5 / 16D, 11 / 16D, 11 / 16D, 11 / 16D, 1);
        arm[Direction.WEST.ordinal()] =
                Shapes.box(0, 5 / 16D, 5 / 16D, 5 / 16D, 11 / 16D, 11 / 16D);
        arm[Direction.EAST.ordinal()] =
                Shapes.box(11 / 16D, 5 / 16D, 5 / 16D, 1, 11 / 16D, 11 / 16D);
        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int o = 0; o < 6; o++) {
                if ((mask & (1 << o)) != 0) shape = Shapes.or(shape, arm[o]);
            }
            shapes[mask] = shape;
        }
        return shapes;
    }

    private static BooleanProperty propertyFor(Direction dir) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(dir);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public boolean hasPowerContact(Level level, BlockPos pos, BlockState state, Direction side) {
        return true;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state =
                defaultBlockState()
                        .setValue(
                                WATERLOGGED,
                                ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
        for (Direction dir : Direction.VALUES) {
            BlockPos npos = ctx.getClickedPos().relative(dir);
            state =
                    state.setValue(
                            propertyFor(dir),
                            PhysicalGraph.powerContact(ctx.getLevel(), npos, dir.getOpposite()));
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
        boolean joins = PhysicalGraph.powerContact(level, pos.relative(dir), dir.getOpposite());
        if (level.isClientSide())
            return PhysicalGraph.predictArm(state, propertyFor(dir), neighbourState, joins);
        return state.setValue(propertyFor(dir), joins);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (Direction dir : Direction.VALUES) {
            if (state.getValue(propertyFor(dir))) mask |= 1 << dir.ordinal();
        }
        return SHAPES[mask];
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel sl && !oldState.is(this)) {
            LevelNodeGraph<CableData> graph = PowerGraph.get(sl);
            graph.addNode(pos.asLong(), CableData.INSTANCE, OPEN_ALL);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PowerGraph.get(level).removeNode(pos.asLong());
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level instanceof ServerLevel sl) {
            LevelNodeGraph.invalidateEndpointsAt(sl, pos);
            PhysicalGraph.redraw(state, sl, pos);
        }
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
