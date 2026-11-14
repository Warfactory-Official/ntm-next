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

package com.hbm.blocks.machine;

import com.hbm.blockentity.generator.IsotopeGeneratorBlockEntity;
import com.hbm.blocks.CapabilityDependentShape;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.BlockMenus;
import com.hbm.uninos.graph.PhysicalGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class RTGBlock extends Block
        implements CapabilityDependentShape.Endpoint, TickingEntityBlock, CapabilityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public RTGBlock(BlockBehaviour.Properties props) {
        super(CapabilityDependentShape.deferShapeToFullChunk(props));
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false));
    }

    private static BooleanProperty propertyFor(Direction dir) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(dir);
    }

    public static int mask(BlockState state) {
        int mask = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (state.getValue(propertyFor(dir))) mask |= 1 << dir.ordinal();
        }
        return mask;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
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
        if (dir.getAxis() == Direction.Axis.Y) return state;
        boolean joins = PhysicalGraph.powerContact(level, pos.relative(dir), dir.getOpposite());
        if (level.isClientSide())
            return PhysicalGraph.predictArm(state, propertyFor(dir), neighbourState, joins);
        return state.setValue(propertyFor(dir), joins);
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
            PhysicalGraph.redraw(state, sl, pos);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IsotopeGeneratorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            BlockMenus.open(player, provider);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.MACHINE_RTG).powerOut().items().fe();
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
