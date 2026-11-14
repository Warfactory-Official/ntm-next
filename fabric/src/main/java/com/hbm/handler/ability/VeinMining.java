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

package com.hbm.handler.ability;

import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.data.ItemData;
import com.hbm.items.tool.ToolAbilityItem;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class VeinMining {

    private static final int NEIGHBOURS = 26;
    private static final int[] OFFSETS = new int[NEIGHBOURS * 3];

    static {
        int at = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx | dy | dz) == 0) continue;
                    OFFSETS[at++] = dx;
                    OFFSETS[at++] = dy;
                    OFFSETS[at++] = dz;
                }
            }
        }
    }

    private final ToolDig dig;
    private final Block block;
    private final int radiusSquared;
    private final int maxDepth;
    private final RandomSource random;
    private final int span;
    private final long[] examined;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    private int[] frameOffsets = new int[16 * 3];
    private int[] frameCursors = new int[16];
    private byte[] frameOrders = new byte[16 * NEIGHBOURS];
    private int top = -1;

    private VeinMining(ToolDig dig, Block block, int radius, int maxDepth) {
        this.dig = dig;
        this.block = block;
        this.radiusSquared = radius * radius;
        this.maxDepth = maxDepth;
        this.random = dig.level().getRandom();
        this.span = 2 * radius + 3;
        this.examined = new long[(span * span * span + 63) >>> 6];
    }

    static boolean run(ToolDig dig, int radius) {
        BlockState reference = dig.level().getBlockState(dig.reference());
        if (reference.is(BlockTags.BASE_STONE_OVERWORLD) && !ItemData.TOOL_RECURSION_STONE.get())
            return false;
        if (reference.is(BlockTags.BASE_STONE_NETHER) && !ItemData.TOOL_RECURSION_NETHERRACK.get())
            return false;
        new VeinMining(
                        dig,
                        reference.getBlock(),
                        radius,
                        ((RuntimeConfig) HbmConfig.RUNTIME).toolRecursionDepth())
                .walk();
        return false;
    }

    private void walk() {
        mark(0, 0, 0);
        push(0, 0, 0);
        var level = dig.level();
        var player = dig.player();
        BlockPos reference = dig.reference();
        while (top >= 0) {
            int frame = top;
            int cursor = frameCursors[frame];
            if (cursor == NEIGHBOURS || frame >= maxDepth) {
                top--;
                continue;
            }
            frameCursors[frame] = cursor + 1;

            int neighbour = frameOrders[frame * NEIGHBOURS + cursor] * 3;
            int dx = frameOffsets[frame * 3] + OFFSETS[neighbour];
            int dy = frameOffsets[frame * 3 + 1] + OFFSETS[neighbour + 1];
            int dz = frameOffsets[frame * 3 + 2] + OFFSETS[neighbour + 2];
            if (!mark(dx, dy, dz)) continue;
            if (dx * dx + dy * dy + dz * dz > radiusSquared) continue;
            if (!level.getBlockState(pos.setWithOffset(reference, dx, dy, dz)).is(block)) continue;
            if (player.getMainHandItem().isEmpty()) return;

            ToolAbilityItem.breakExtraBlock(dig, pos);
            push(dx, dy, dz);
        }
    }

    private boolean mark(int dx, int dy, int dz) {
        int half = span >> 1;
        int bit = ((dx + half) * span + dy + half) * span + dz + half;
        long mask = 1L << bit;
        if ((examined[bit >>> 6] & mask) != 0) return false;
        examined[bit >>> 6] |= mask;
        return true;
    }

    private void push(int dx, int dy, int dz) {
        int frame = ++top;
        if (frame == frameCursors.length) {
            frameOffsets = Arrays.copyOf(frameOffsets, frameOffsets.length * 2);
            frameCursors = Arrays.copyOf(frameCursors, frameCursors.length * 2);
            frameOrders = Arrays.copyOf(frameOrders, frameOrders.length * 2);
        }
        frameOffsets[frame * 3] = dx;
        frameOffsets[frame * 3 + 1] = dy;
        frameOffsets[frame * 3 + 2] = dz;
        frameCursors[frame] = 0;
        int base = frame * NEIGHBOURS;
        for (int i = 0; i < NEIGHBOURS; i++) frameOrders[base + i] = (byte) i;
        for (int i = NEIGHBOURS - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            byte swap = frameOrders[base + i];
            frameOrders[base + i] = frameOrders[base + j];
            frameOrders[base + j] = swap;
        }
    }
}
