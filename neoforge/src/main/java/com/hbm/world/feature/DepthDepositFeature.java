/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record DepthDepositFeature(
        BlockState core, BlockState filler, RuleTest replaceable, int size, float fill)
        implements Feature {
    public static final MapCodec<DepthDepositFeature> CODEC =
            RecordCodecBuilder.mapCodec(
                    inst ->
                            inst.group(
                                            BlockState.CODEC
                                                    .fieldOf("core")
                                                    .forGetter(DepthDepositFeature::core),
                                            BlockState.CODEC
                                                    .fieldOf("filler")
                                                    .forGetter(DepthDepositFeature::filler),
                                            RuleTest.CODEC
                                                    .fieldOf("replaceable")
                                                    .forGetter(DepthDepositFeature::replaceable),
                                            Codec.INT
                                                    .fieldOf("size")
                                                    .forGetter(DepthDepositFeature::size),
                                            Codec.FLOAT
                                                    .fieldOf("fill")
                                                    .forGetter(DepthDepositFeature::fill))
                                    .apply(inst, DepthDepositFeature::new));

    @Override
    public MapCodec<DepthDepositFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        DepthDepositFeature cfg = this;
        RandomSource rand = random;
        BlockPos origin = featureOrigin;

        int size = cfg.size();
        double coreRadius = size * cfg.fill();
        WorldGenerationContext generation = new WorldGenerationContext(chunkGenerator, level);
        int floor = generation.getMinGenY();
        int ceiling = floor + generation.getGenDepth() - 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -size; dx <= size; dx++) {
            for (int dy = -size; dy <= size; dy++) {
                int y = origin.getY() + dy;
                if (y <= floor || y >= ceiling) continue;
                for (int dz = -size; dz <= size; dz++) {
                    if (dx * dx + dy * dy + dz * dz > size * size) continue;
                    pos.set(origin.getX() + dx, y, origin.getZ() + dz);
                    BlockState cur = level.getBlockState(pos);
                    if (!cfg.replaceable().test(cur, (BlockPos) pos, rand)
                            && !cur.is(Blocks.BEDROCK)) continue;

                    double len = Mth.length(dx, dy, dz);
                    if (len + rand.nextInt(2) < coreRadius) {
                        level.setBlock(pos, cfg.core(), 2);
                    } else if (len + rand.nextInt(2) <= size) {
                        level.setBlock(pos, cfg.filler(), 2);
                    }
                }
            }
        }
        return true;
    }
}
