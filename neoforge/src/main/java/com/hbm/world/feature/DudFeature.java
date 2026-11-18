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
import com.hbm.blocks.bomb.CrashedBombBlock.EnumDudType;
import com.hbm.world.LocationIsValidSpawn;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public class DudFeature implements Feature {
    public static final MapCodec<DudFeature> CODEC = MapCodec.unit(DudFeature::new);

    private static final EnumDudType[] DUD_TYPES = EnumDudType.values();

    @Override
    public MapCodec<DudFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        RandomSource rand = random;
        BlockPos pos = featureOrigin;

        if (!level.getBlockState(pos).isAir()
                || !LocationIsValidSpawn.isValidGround(
                        level.getBlockState(pos.below()), level.getBlockState(pos.below(2))))
            return false;

        EnumDudType type = DUD_TYPES[rand.nextInt(DUD_TYPES.length)];
        level.setBlock(
                pos, ModBlocks.CRASHED_BOMBS.get(type.ordinal()).get().defaultBlockState(), 3);
        return true;
    }
}
