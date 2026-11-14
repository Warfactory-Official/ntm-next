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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class FleijaColumnProcessor implements ExplosionColumnProcessor {

    public static final FleijaColumnProcessor INSTANCE = new FleijaColumnProcessor();

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private FleijaColumnProcessor() {}

    static int halfHeight(int radius, int x, int z) {
        int span = radius * radius - x * x - z * z;
        return span <= 0 ? -1 : (int) Math.sqrt(span);
    }

    @Override
    public void column(SpiralExplosion sweep, ServerLevel level, int x, int z) {
        int half = halfHeight(sweep.getRadius(), x, z);
        if (half < 0) return;
        int cx = sweep.getOriginX() + x;
        int cz = sweep.getOriginZ() + z;
        int bottom = Math.max(sweep.getOriginY() - half + 1, level.getMinY() + 1);
        int top = Math.min(sweep.getOriginY() + half, level.getMaxY());
        if (top < bottom) return;
        var chunk = level.getChunk(cx >> 4, cz >> 4);
        var cursor = sweep.getCursor();
        var statue = ModBlocks.STATUE_ELB_F.get();
        for (int y = top; y >= bottom; y--) {
            var state = chunk.getBlockState(cursor.set(cx, y, cz));
            if (state != AIR && !state.is(statue)) level.setBlock(cursor, AIR, Block.UPDATE_ALL);
        }
    }
}
