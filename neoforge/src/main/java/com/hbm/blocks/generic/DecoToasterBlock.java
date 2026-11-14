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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DecoToasterBlock extends Block {
    public static final MapCodec<DecoToasterBlock> CODEC = simpleCodec(DecoToasterBlock::new);
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

    private static final VoxelShape EVEN = Shapes.box(0.25, 0, 0.375, 0.75, 0.325, 0.625);
    private static final VoxelShape ODD = Shapes.box(0.375, 0, 0.25, 0.625, 0.325, 0.75);

    public DecoToasterBlock(Properties props) {
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
