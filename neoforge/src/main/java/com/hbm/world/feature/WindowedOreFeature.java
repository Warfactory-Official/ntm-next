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

import com.hbm.world.NtmWorldgenFields;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;

public record WindowedOreFeature(
        OreFeature vein,
        int attempts,
        int minY,
        int yRange,
        int windowSize,
        int minimumCenterRadius,
        int maximumCenterRadius)
        implements Feature {
    public static final MapCodec<WindowedOreFeature> CODEC =
            RecordCodecBuilder.mapCodec(
                    inst ->
                            inst.group(
                                            OreFeature.CODEC
                                                    .codec()
                                                    .fieldOf("vein")
                                                    .forGetter(WindowedOreFeature::vein),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("attempts")
                                                    .forGetter(WindowedOreFeature::attempts),
                                            Codec.INT
                                                    .fieldOf("min_y")
                                                    .forGetter(WindowedOreFeature::minY),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("y_range")
                                                    .forGetter(WindowedOreFeature::yRange),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("window_size")
                                                    .forGetter(WindowedOreFeature::windowSize),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("minimum_center_radius")
                                                    .forGetter(
                                                            WindowedOreFeature
                                                                    ::minimumCenterRadius),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("maximum_center_radius")
                                                    .forGetter(
                                                            WindowedOreFeature
                                                                    ::maximumCenterRadius))
                                    .apply(inst, WindowedOreFeature::new));

    public WindowedOreFeature {
        if (minimumCenterRadius > maximumCenterRadius) {
            throw new IllegalArgumentException("minimum center radius exceeds maximum");
        }
    }

    @Override
    public MapCodec<WindowedOreFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        WindowedOreFeature cfg = this;
        RandomSource rand = random;
        int cx = featureOrigin.getX() & ~15;
        int cz = featureOrigin.getZ() & ~15;
        NtmWorldgenFields fields = NtmWorldgenFields.get(level);
        long center = fields.australiumCenter(cfg.minimumCenterRadius(), cfg.maximumCenterRadius());
        int centerX = (int) (center >> 32);
        int centerZ = (int) center;
        int lowerHalf = cfg.windowSize() / 2;
        int minX = centerX - lowerHalf;
        int minZ = centerZ - lowerHalf;
        int maxX = minX + cfg.windowSize() - 1;
        int maxZ = minZ + cfg.windowSize() - 1;
        if (cx + 15 < minX || cx > maxX || cz + 15 < minZ || cz > maxZ) return true;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int attemptCount = rand.nextInt(cfg.attempts());
        for (int k = 0; k < attemptCount; k++) {
            int x = cx + rand.nextInt(16);
            int y = cfg.minY() + rand.nextInt(cfg.yRange());
            int z = cz + rand.nextInt(16);
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                cfg.vein().place(level, chunkGenerator, rand, pos.set(x, y, z));
            }
        }
        return true;
    }
}
