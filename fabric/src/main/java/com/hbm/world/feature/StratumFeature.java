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
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class StratumFeature extends Feature<StratumConfig> {
    private static final int SIZE = 16;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    public StratumFeature() {
        super(StratumConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<StratumConfig> ctx) {
        WorldGenLevel level = ctx.level();
        StratumConfig cfg = ctx.config();
        RandomSource rand = ctx.random();

        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;

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
                    if (cfg.replaceable().test(reader.read(x, y, z), rand)) {
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
