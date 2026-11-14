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

import com.hbm.api.foundry.FoundryChannelGraph;
import com.hbm.blockentity.machine.FoundryChannelBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.items.machine.ScrapsItem;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class FoundryChannelBlock extends Block
        implements TickingEntityBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;

    public FoundryChannelBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(NORTH, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(EAST, false)
                        .setValue(WATERLOGGED, false));
    }

    public static @Nullable BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            case DOWN, UP -> null;
        };
    }

    public static boolean canConnectTo(BlockGetter level, BlockPos pos, Direction direction) {
        if (direction.getAxis().isVertical()) return false;

        var neighbor = level.getBlockState(pos.relative(direction));
        if (neighbor.getBlock() instanceof FoundryOutletBlock) {
            return neighbor.getValue(FoundryOutletBlock.FACING) == direction;
        }
        return neighbor.is(ModBlocks.FOUNDRY_CHANNEL.get())
                || neighbor.is(ModBlocks.FOUNDRY_MOLD.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, SOUTH, WEST, EAST, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(NORTH, canConnectTo(level, pos, Direction.NORTH))
                .setValue(SOUTH, canConnectTo(level, pos, Direction.SOUTH))
                .setValue(WEST, canConnectTo(level, pos, Direction.WEST))
                .setValue(EAST, canConnectTo(level, pos, Direction.EAST))
                .setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED))
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (direction.getAxis().isVertical()) return state;
        return state.setValue(property(direction), canConnectTo(level, pos, direction));
    }

    private static int connectionMask(BlockState state) {
        int mask = 0;
        if (state.getValue(NORTH)) mask |= 1;
        if (state.getValue(SOUTH)) mask |= 2;
        if (state.getValue(WEST)) mask |= 4;
        if (state.getValue(EAST)) mask |= 8;
        return mask;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ReactorBlockData.CHANNEL_SELECTION[connectionMask(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ReactorBlockData.CHANNEL_COLLISION[connectionMask(state)];
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            FoundryChannelGraph.get(server)
                    .addNode(pos.asLong(), null, FoundryChannelGraph.OPEN_HORIZONTALS);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        FoundryChannelGraph.get(level).removeNode(pos.asLong());
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!held.is(ItemTags.SHOVELS)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FoundryChannelBlockEntity channel)) {
            return InteractionResult.PASS;
        }

        if (channel.amount > 0 && channel.type != null) {
            var scraps = ScrapsItem.create(new MaterialStack(channel.type, channel.amount));
            channel.hbm$syncSet$FoundryBaseBlockEntity$amount(0);
            channel.hbm$syncSet$FoundryBaseBlockEntity$type(null);
            channel.setChanged();
            player.getInventory().placeItemBackInInventory(scraps, Prediction.SERVER_ONLY);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundryChannelBlockEntity(pos, state);
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
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED)) return Fluids.WATER.getSource(false);
        return super.getFluidState(state);
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
