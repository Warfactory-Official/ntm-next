/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.network;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ConveyorChuteBlock extends BaseConveyorBlock {
    public static final BooleanProperty FEEDS_DOWN = BooleanProperty.create("feeds_down");

    public static boolean feedsOn(BlockGetter level, BlockPos pos) {
        return ConveyorNeighbors.receiverAt(level, pos.below());
    }

    public ConveyorChuteBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FEEDS_DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FEEDS_DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        boolean feedsDown = feedsOn(context.getLevel(), context.getClickedPos());
        return state.setValue(FEEDS_DOWN, feedsDown);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction side,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (side == Direction.DOWN) {
            return state.setValue(FEEDS_DOWN, ConveyorNeighbors.receiverAt(level, neighborPos));
        }
        return super.updateShape(
                state, level, ticks, pos, side, neighborPos, neighborState, random);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public Direction getInputDirection(Level level, BlockPos pos) {
        return Direction.UP;
    }

    @Override
    public Direction getOutputDirection(Level level, BlockPos pos) {
        return Direction.DOWN;
    }

    @Override
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        if (level.getBlockState(pos).getValue(FEEDS_DOWN) || itemPos.y > pos.getY() + 0.25) {
            return Direction.UP;
        }
        return super.getTravelDirection(level, pos, itemPos);
    }

    @Override
    public Vec3 getTravelPosition(Level level, BlockPos pos, Vec3 rider, double speed) {
        if (level.getBlockState(pos).getValue(FEEDS_DOWN)) {
            speed *= 5;
        } else if (rider.y > pos.getY() + 0.25) {
            speed *= 3;
        }
        return super.getTravelPosition(level, pos, rider, speed);
    }

    @Override
    protected Snap snap(Level level, BlockPos pos, Vec3 itemPos) {
        if (level.getBlockState(pos).getValue(FEEDS_DOWN) || itemPos.y > pos.getY() + 0.25) {
            var lane = new Vec3(pos.getX() + 0.5, itemPos.y, pos.getZ() + 0.5);
            return new Snap(lane, itemPos);
        }
        return super.snap(level, pos, itemPos);
    }

    @Override
    protected void onSneakScrew(Level level, BlockPos pos, BlockState state) {
        var regular =
                ModBlocks.CONVEYOR
                        .get()
                        .defaultBlockState()
                        .setValue(FACING, state.getValue(FACING));
        level.setBlock(pos, regular, Block.UPDATE_ALL);
    }
}
