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

import com.hbm.blockentity.machine.ChargerBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ChargerBlock extends HorizontalDirectionalBlock
        implements TickingEntityBlock, CapabilityBlock {
    private final VoxelShape[] shapesByFacing = new VoxelShape[4];

    public ChargerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            shapesByFacing[dir.get2DDataValue()] = buildShape(dir);
        }
    }

    private static VoxelShape buildShape(Direction facing) {
        double lo = 5.0 / 16, hi = 11.0 / 16, yLo = 0.25, yHi = 0.75, thin = 4.0 / 16;
        return switch (facing.getOpposite()) {
            case NORTH -> Shapes.box(lo, yLo, 0, hi, yHi, thin);
            case SOUTH -> Shapes.box(lo, yLo, 1 - thin, hi, yHi, 1);
            case WEST -> Shapes.box(0, yLo, lo, thin, yHi, hi);
            case EAST -> Shapes.box(1 - thin, yLo, lo, 1, yHi, hi);
            default -> Shapes.box(lo, yLo, lo, hi, yHi, hi);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapesByFacing[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargerBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.NTM_CHARGER)
                .powerIn()
                .fe()
                .faces(ChargerBlockEntity.class, ChargerBlockEntity::acceptsFace);
    }
}
