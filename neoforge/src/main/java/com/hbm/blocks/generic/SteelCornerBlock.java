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

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SteelCornerBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SteelCornerBlock> CODEC = simpleCodec(SteelCornerBlock::new);

    private static final VoxelShape SHAPE_NORTH =
            Shapes.or(
                    Shapes.box(4 / 16D, 0D, 14 / 16D, 1D, 1D, 1D),
                    Shapes.box(0D, 0D, 12 / 16D, 4 / 16D, 1D, 1D),
                    Shapes.box(0D, 0D, 0D, 2 / 16D, 1D, 12 / 16D));

    private static final VoxelShape SHAPE_EAST = rotateY(SHAPE_NORTH);
    private static final VoxelShape SHAPE_SOUTH = rotateY(SHAPE_EAST);
    private static final VoxelShape SHAPE_WEST = rotateY(SHAPE_SOUTH);

    public SteelCornerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static VoxelShape rotateY(VoxelShape shape) {
        VoxelShape[] out = {Shapes.empty()};
        shape.forAllBoxes(
                (minX, minY, minZ, maxX, maxY, maxZ) ->
                        out[0] =
                                Shapes.or(
                                        out[0],
                                        Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
        return out[0];
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.getValue(FACING)) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state);
    }
}
