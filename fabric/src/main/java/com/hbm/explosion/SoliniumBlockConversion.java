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

package com.hbm.explosion;

import com.hbm.blocks.ModBlocks;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SoliniumBlockConversion {

    private static final int UNCHANGED = 0;
    private static final int DIRT = 1;
    private static final int REMOVE = 2;

    static final BlockStateClassificationCache KINDS =
            new BlockStateClassificationCache(SoliniumBlockConversion::classify);

    private static Claim[] claims = {};

    private SoliniumBlockConversion() {}

    @FunctionalInterface
    public interface Claim {
        boolean claims(Level level, BlockPos pos, BlockState state);
    }

    public static void claim(Claim claim) {
        claims = Arrays.copyOf(claims, claims.length + 1);
        claims[claims.length - 1] = claim;
    }

    public static void apply(Level level, BlockPos pos) {
        apply(level, pos, level.getBlockState(pos));
    }

    public static void apply(Level level, BlockPos pos, BlockState state) {
        for (var claim : claims) {
            if (claim.claims(level, pos, state)) return;
        }
        switch (KINDS.of(state)) {
            case DIRT -> level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            case REMOVE -> level.removeBlock(pos, false);
            default -> {}
        }
    }

    private static int classify(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(ModBlocks.WASTE_EARTH.get())
                || state.is(ModBlocks.WASTE_MYCELIUM.get())) {
            return DIRT;
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.LEAVES)
                || state.getBlock() instanceof VegetationBlock
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.SPONGE)
                || state.is(Blocks.WET_SPONGE)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(ModBlocks.PLANT_REEDS.get())
                || state.is(ModBlocks.LEAVES_LAYER.get())
                || state.is(ModBlocks.GLYPHID_BASE.get())
                || state.is(ModBlocks.GLYPHID_BASE_INFESTED.get())
                || state.is(ModBlocks.GLYPHID_BASE_RAD.get())
                || state.is(ModBlocks.GLYPHID_SPAWNER.get())
                || state.is(ModBlocks.GLYPHID_SPAWNER_INFESTED.get())
                || state.is(ModBlocks.GLYPHID_SPAWNER_RAD.get())) {
            return REMOVE;
        }
        return UNCHANGED;
    }
}
