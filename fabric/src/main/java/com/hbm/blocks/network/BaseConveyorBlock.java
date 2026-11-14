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

import com.hbm.api.conveyor.ConveyorBelt;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BaseConveyorBlock extends Block
        implements ConveyorBelt, ToolInteractionHandler {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected BaseConveyorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TransportBlockData.CONVEYOR_SLAB;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TransportBlockData.CONVEYOR_SLAB;
    }

    public Direction getInputDirection(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    public Direction getOutputDirection(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(FACING);
    }

    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 rider) {
        return true;
    }

    @Override
    public Vec3 getTravelPosition(Level level, BlockPos pos, Vec3 rider, double speed) {
        var points = snap(level, pos, rider);
        var against = getTravelDirection(level, pos, rider);
        return BeltTravel.step(points.item(), points.snap(), against, speed);
    }

    @Override
    public Vec3 getSnappingPosition(Level level, BlockPos pos, Vec3 near) {
        return snap(level, pos, near).snap();
    }

    protected Snap snap(Level level, BlockPos pos, Vec3 itemPos) {
        var against = getTravelDirection(level, pos, itemPos);
        var clamped = BeltTravel.clamp(pos, itemPos);
        var lane = BeltTravel.centreLine(pos, clamped, against);
        return new Snap(lane, clamped);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (level.isClientSide()
                || !(entity instanceof ItemEntity loose)
                || loose.isRemoved()
                || loose.tickCount <= 10) return;

        var moving = new MovingItemEntity(level);
        moving.setItemStack(loose.getItem().copy());
        moving.setPos(getSnappingPosition(level, pos, loose.position()));
        level.addFreshEntity(moving);
        loose.discard();
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction face,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER || player != null && player.isSpectator()) return false;
        if (level.isClientSide()) return true;

        var state = level.getBlockState(pos);
        if (player != null && player.isSecondaryUseActive()) {
            onSneakScrew(level, pos, state);
        } else {
            var turned = state.setValue(FACING, state.getValue(FACING).getClockWise());
            level.setBlock(pos, turned, Block.UPDATE_ALL);
        }
        return true;
    }

    protected abstract void onSneakScrew(Level level, BlockPos pos, BlockState state);

    protected record Snap(Vec3 snap, Vec3 item) {}
}
