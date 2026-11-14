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

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.LanternBehemothBlock;
import com.hbm.itempool.LootPile;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class LanternBehemothFeature implements Feature {
    public static final MapCodec<LanternBehemothFeature> CODEC =
            MapCodec.unit(LanternBehemothFeature::new);

    @Override
    public MapCodec<LanternBehemothFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        BlockPos pos = featureOrigin;
        if (!Block.canSupportCenter(level, pos.below(), Direction.UP)
                || !isReplaceable(level.getBlockState(pos))) return false;

        LanternBehemothBlock block = ModBlocks.LANTERN_BEHEMOTH.get();
        BlockState state =
                block.defaultBlockState()
                        .setValue(LanternBehemothBlock.FACING, Direction.NORTH)
                        .setValue(LanternBehemothBlock.BROKEN, true);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        block.fillSpace(level, pos, Direction.NORTH);

        if (random.nextInt(2) == 0) {
            BlockPos loot = pos.north(2);
            LootPile.BOOKLET.place(level, loot, random, level.getSeed());
        }
        return true;
    }

    public static boolean isReplaceable(BlockState state) {
        return state.canBeReplaced() && !(state.getBlock() instanceof DoublePlantBlock);
    }
}
