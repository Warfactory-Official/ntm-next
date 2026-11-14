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

import com.hbm.tags.HbmBlockTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MetalFenceBlock extends FenceBlock {
    public static final MapCodec<FenceBlock> CODEC = simpleCodec(MetalFenceBlock::new);

    private static final double MIN = 6 / 16D;
    private static final double MAX = 10 / 16D;

    public MetalFenceBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<FenceBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean connectsTo(BlockState state, boolean faceSolid, Direction direction) {
        return state.is(HbmBlockTags.METAL_FENCE_JOINS)
                || super.connectsTo(state, faceSolid, direction);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        double minX = state.getValue(WEST) ? 0D : MIN;
        double maxX = state.getValue(EAST) ? 1D : MAX;
        double minZ = state.getValue(NORTH) ? 0D : MIN;
        double maxZ = state.getValue(SOUTH) ? 1D : MAX;
        return Shapes.box(minX, 0D, minZ, maxX, 1D, maxZ);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        boolean n = state.getValue(NORTH), s = state.getValue(SOUTH);
        boolean e = state.getValue(EAST), w = state.getValue(WEST);
        VoxelShape shape = Shapes.empty();

        if (n || s) {
            shape = Shapes.or(shape, Shapes.box(MIN, 0D, n ? 0D : MIN, MAX, 1D, s ? 1D : MAX));
        }
        if (e || w || (!n && !s)) {
            shape = Shapes.or(shape, Shapes.box(w ? 0D : MIN, 0D, MIN, e ? 1D : MAX, 1D, MAX));
        }
        return shape;
    }
}
