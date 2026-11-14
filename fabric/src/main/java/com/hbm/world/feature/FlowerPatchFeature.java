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

import com.hbm.saveddata.TomSaveData;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public record FlowerPatchFeature(Block flower, int odds) implements Feature {
    public static final MapCodec<FlowerPatchFeature> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            BuiltInRegistries.BLOCK
                                                    .byNameCodec()
                                                    .fieldOf("flower")
                                                    .forGetter(FlowerPatchFeature::flower),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("odds")
                                                    .forGetter(FlowerPatchFeature::odds))
                                    .apply(instance, FlowerPatchFeature::new));

    @Override
    public MapCodec<FlowerPatchFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos origin) {
        if (TomSaveData.impact(level.getLevel()) || random.nextInt(odds) != 0) return false;

        BlockPos chunkOrigin = origin;
        int x = (chunkOrigin.getX() & ~15) + random.nextInt(16);
        int z = (chunkOrigin.getZ() & ~15) + random.nextInt(16);
        int y = WorldgenHeight.lightBlocking(level, x, z);
        BlockState flower = this.flower.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int attempt = 0; attempt < 64; attempt++) {
            pos.set(
                    x + random.nextInt(8) - random.nextInt(8),
                    y + random.nextInt(4) - random.nextInt(4),
                    z + random.nextInt(8) - random.nextInt(8));
            if (level.isEmptyBlock(pos) && flower.canSurvive(level, pos)) {
                level.setBlock(pos, flower, 2);
                placed = true;
            }
        }
        return placed;
    }
}
