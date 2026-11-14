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

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class RedBrickBlock extends Block {
    public static final IntegerProperty FACE = IntegerProperty.create("face", 0, 6);

    public RedBrickBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(
                        FACE, context.getNearestLookingDirection().getOpposite().get3DDataValue());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        int face = state.getValue(FACE);
        return face == 6
                ? state
                : state.setValue(
                        FACE, rotation.rotate(Direction.from3DDataValue(face)).get3DDataValue());
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        int face = state.getValue(FACE);
        return face == 6
                ? state
                : state.rotate(mirror.getRotation(Direction.from3DDataValue(face)));
    }
}
