// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.world.NtmWorldgenFields;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
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

        int index = 0;
        for (int x = cx; x < cx + SIZE; x++) {
            for (int z = cz; z < cz + SIZE; z++) {

                double n = noiseValues[index++];
                if (n <= cfg.threshold()) continue;

                int range = (int) ((n - cfg.threshold()) * cfg.rangeMult());
                if (range > cfg.maxRange()) range = cfg.maxRange() * 2 - range;
                if (range < 0) continue;

                for (int y = cfg.yLevel() - range; y <= cfg.yLevel() + range; y++) {

                    if (cfg.density() < 1.0F && rand.nextFloat() >= cfg.density()) continue;
                    pos.set(x, y, z);
                    if (cfg.replaceable().test(level.getBlockState(pos), rand)) {
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
