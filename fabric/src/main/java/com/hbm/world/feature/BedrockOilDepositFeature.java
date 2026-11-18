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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record BedrockOilDepositFeature(
        BlockState core, RuleTest replaceable, List<BlockReplacement> surfaceScars)
        implements Feature {

    public static final MapCodec<BedrockOilDepositFeature> CODEC =
            RecordCodecBuilder.mapCodec(
                    inst ->
                            inst.group(
                                            BlockState.CODEC
                                                    .fieldOf("core")
                                                    .forGetter(BedrockOilDepositFeature::core),
                                            RuleTest.CODEC
                                                    .fieldOf("replaceable")
                                                    .forGetter(
                                                            BedrockOilDepositFeature::replaceable),
                                            BlockReplacement.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("surface_scars", List.of())
                                                    .forGetter(
                                                            BedrockOilDepositFeature::surfaceScars))
                                    .apply(inst, BedrockOilDepositFeature::new));

    public BedrockOilDepositFeature(BlockState core, RuleTest replaceable) {
        this(core, replaceable, List.of());
    }

    @Override
    public MapCodec<BedrockOilDepositFeature> codec() {
        return CODEC;
    }

    private static final int DXZ_LIMIT = 4;
    public static final int MAX_Y = 4;
    private static final int L1_MAX = 6;
    private static final int OIL_SPOT_WIDTH = 5;
    private static final int OIL_SPOT_COUNT = 50;

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        int centerX = featureOrigin.getX();
        int centerZ = featureOrigin.getZ();
        boolean placedAny = placeNode(level, this, random, centerX, centerZ);
        OilSurfaceScarring.addBedrockOilSpot(
                level,
                random,
                centerX,
                centerZ,
                OIL_SPOT_WIDTH,
                OIL_SPOT_COUNT,
                this.surfaceScars());
        return placedAny;
    }

    public static boolean placeNode(
            WorldGenLevel level,
            BedrockOilDepositFeature cfg,
            RandomSource random,
            int centerX,
            int centerZ) {
        int floor = level.getMinY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placedAny = false;
        for (int dx = -DXZ_LIMIT; dx <= DXZ_LIMIT; dx++) {
            for (int dy = 0; dy <= MAX_Y; dy++) {
                for (int dz = -DXZ_LIMIT; dz <= DXZ_LIMIT; dz++) {
                    if (Math.abs(dx) + dy + Math.abs(dz) > L1_MAX) continue;
                    pos.set(centerX + dx, floor + dy, centerZ + dz);
                    BlockState cur = level.getBlockState(pos);
                    if (!cfg.replaceable().test(cur, (BlockPos) pos, random)
                            && !cur.is(Blocks.BEDROCK)) continue;
                    level.setBlock(pos, cfg.core(), 2);
                    placedAny = true;
                }
            }
        }
        return placedAny;
    }
}
