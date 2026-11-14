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

package com.hbm.explosion.spiral;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class BalefireColumnProcessor implements ExplosionColumnProcessor {

    public static final BalefireColumnProcessor INSTANCE = new BalefireColumnProcessor();

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final int SLAKED_DEPTH = 5;

    private BalefireColumnProcessor() {}

    @Override
    public void column(SpiralExplosion sweep, ServerLevel level, int x, int z) {
        int radius = sweep.getRadius();
        int amplitude = (int) (radius - Math.sqrt(x * x + z * z));
        if (amplitude <= 0) return;
        int cx = sweep.getOriginX() + x;
        int cz = sweep.getOriginZ() + z;
        var cursor = sweep.getCursor();
        int minY = level.getMinY();
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
        LevelChunk chunk = null;
        int surface = minY;
        if (top > minY) {
            chunk = level.getChunk(cx >> 4, cz >> 4);
            surface = surface(chunk, cursor, cx, top - 1, cz, minY);
        }
        int most = (int) (10 + 0.25 * radius);
        int depth = (int) (most * amplitude / radius + 2 * Math.sin(0.15 * amplitude + 2));
        int floor = Math.max(surface - depth, minY);
        var random = level.getRandom();
        var cluster = ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get();
        var balefire = ModBlocks.BALEFIRE.get().defaultBlockState();
        for (int y = surface; y > floor; y--) {
            var state = chunk.getBlockState(cursor.set(cx, y, cz));
            if (state.is(cluster)) {
                if (random.nextInt(10) == 0) {
                    level.setBlock(cursor.set(cx, y + 1, cz), balefire, Block.UPDATE_ALL);
                    level.setBlock(cursor.set(cx, y, cz), euphemium(state), Block.UPDATE_ALL);
                }
                return;
            }
            if (state != AIR) level.setBlock(cursor, AIR, Block.UPDATE_ALL);
        }
        if (random.nextInt(10) == 0) {
            level.setBlock(cursor.set(cx, floor + 1, cz), balefire, Block.UPDATE_ALL);
            var bottom = level.getBlockState(cursor.set(cx, floor, cz));
            if (bottom.is(cluster)) level.setBlock(cursor, euphemium(bottom), Block.UPDATE_ALL);
        }
        var slaked = ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState();
        for (int y = floor; y > floor - SLAKED_DEPTH; y--) {
            if (slakes(level.getBlockState(cursor.set(cx, y, cz))))
                level.setBlock(cursor, slaked, Block.UPDATE_ALL);
        }
    }

    public static int surface(Level level, BlockPos.MutableBlockPos cursor, int x, int z) {
        int minY = level.getMinY();
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        return top > minY
                ? surface(level.getChunk(x >> 4, z >> 4), cursor, x, top - 1, z, minY)
                : minY;
    }

    private static int surface(
            LevelChunk chunk, BlockPos.MutableBlockPos cursor, int x, int from, int z, int minY) {
        for (int y = from; y >= minY; y--) {
            if (chunk.getBlockState(cursor.set(x, y, z)).getLightDampening() > 0) return y + 1;
        }
        return minY;
    }

    public static boolean slakes(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.DRIPSTONE_BLOCK)
                || state.is(Blocks.SULFUR)
                || state.is(Blocks.CINNABAR)
                || state.is(Blocks.SMOOTH_BASALT);
    }

    private static BlockState euphemium(BlockState cluster) {
        return ModBlocks.BLOCK_EUPHEMIUM_CLUSTER
                .get()
                .defaultBlockState()
                .setValue(BlockStateProperties.AXIS, cluster.getValue(BlockStateProperties.AXIS));
    }
}
