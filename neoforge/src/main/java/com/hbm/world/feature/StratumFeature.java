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

import com.hbm.util.SectionAccess;
import com.hbm.world.NtmWorldgenFields;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record StratumFeature(
        Identifier domain,
        BlockState state,
        RuleTest replaceable,
        double scale,
        int threshold,
        int rangeMult,
        int maxRange,
        int yLevel,
        float density)
        implements Feature {

    public static final MapCodec<StratumFeature> CODEC =
            RecordCodecBuilder.mapCodec(
                    inst ->
                            inst.group(
                                            Identifier.CODEC
                                                    .fieldOf("domain")
                                                    .forGetter(StratumFeature::domain),
                                            BlockState.CODEC
                                                    .fieldOf("state")
                                                    .forGetter(StratumFeature::state),
                                            RuleTest.CODEC
                                                    .fieldOf("replaceable")
                                                    .forGetter(StratumFeature::replaceable),
                                            Codec.DOUBLE
                                                    .fieldOf("scale")
                                                    .forGetter(StratumFeature::scale),
                                            Codec.INT
                                                    .fieldOf("threshold")
                                                    .forGetter(StratumFeature::threshold),
                                            Codec.INT
                                                    .fieldOf("range_mult")
                                                    .forGetter(StratumFeature::rangeMult),
                                            Codec.INT
                                                    .fieldOf("max_range")
                                                    .forGetter(StratumFeature::maxRange),
                                            Codec.INT
                                                    .fieldOf("y_level")
                                                    .forGetter(StratumFeature::yLevel),
                                            Codec.FLOAT
                                                    .fieldOf("density")
                                                    .forGetter(StratumFeature::density))
                                    .apply(inst, StratumFeature::new));

    @Override
    public MapCodec<StratumFeature> codec() {
        return CODEC;
    }

    private static final int SIZE = 16;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        StratumFeature cfg = this;
        RandomSource rand = random;

        int cx = featureOrigin.getX() & ~15;
        int cz = featureOrigin.getZ() & ~15;

        FractalSimplexNoise noise = NtmWorldgenFields.get(level).fractal(cfg.domain(), 4);

        Scratch scratch = SCRATCH.get();
        double[] noiseValues = scratch.noiseValues;
        noise.getGrid(cx, cz, SIZE, SIZE, cfg.scale(), cfg.scale(), noiseValues);
        BlockPos.MutableBlockPos pos = scratch.pos;
        var reader =
                new SectionAccess.Reader() {
                    @Override
                    protected ChunkAccess resolve(int chunkX, int chunkZ) {
                        return level.getChunk(chunkX, chunkZ);
                    }
                };

        int index = 0;
        for (int x = cx; x < cx + SIZE; x++) {
            for (int z = cz; z < cz + SIZE; z++) {

                double n = noiseValues[index++];
                int range =
                        NoiseBand.halfHeight(n, cfg.threshold(), cfg.rangeMult(), cfg.maxRange());
                if (range < 0) continue;

                for (int y = cfg.yLevel() - range; y <= cfg.yLevel() + range; y++) {
                    if (cfg.density() < 1.0F && rand.nextFloat() >= cfg.density()) continue;
                    pos.set(x, y, z);
                    if (cfg.replaceable().test(reader.read(x, y, z), (BlockPos) pos, rand)) {
                        level.setBlock(pos, cfg.state(), 2);
                    }
                }
            }
        }
        return true;
    }

    private static final class Scratch {
        private final double[] noiseValues = new double[SIZE * SIZE];
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    }
}
