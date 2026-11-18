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
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class KeyholeFeature implements Feature {
    public static final MapCodec<KeyholeFeature> CODEC = MapCodec.unit(KeyholeFeature::new);

    @Override
    public MapCodec<KeyholeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        if (random.nextInt(4) != 0) return false;
        int x = (featureOrigin.getX() & ~15) + random.nextInt(16);
        int y = 6 + random.nextInt(13);
        int z = (featureOrigin.getZ() & ~15) + random.nextInt(16);
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD)) return false;
        return level.setBlock(pos, ModBlocks.STONE_KEYHOLE.get().defaultBlockState(), 2);
    }
}
