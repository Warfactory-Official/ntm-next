// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.world.NtmWorldgenFields;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class Stratum3DFeature extends Feature<Stratum3DConfig> {

    private static final int SIZE = 16;
    public static final int MIN_Y = 6;
    public static final int MAX_Y = 64;
    private static final int SPAN = MAX_Y - MIN_Y + 1;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    public Stratum3DFeature() {
        super(Stratum3DConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Stratum3DConfig> ctx) {
        WorldGenLevel level = ctx.level();
        Stratum3DConfig cfg = ctx.config();
        RandomSource rand = ctx.random();

        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;

        NtmWorldgenFields fields = NtmWorldgenFields.get(level);
        FractalSimplexNoise noiseX = fields.fractal(cfg.domainX(), 4);
        FractalSimplexNoise noiseY = fields.fractal(cfg.domainY(), 4);
        FractalSimplexNoise noiseZ = fields.fractal(cfg.domainZ(), 4);

        double sh = cfg.scaleH();
        double sv = cfg.scaleV();
        double threshold = cfg.threshold();
        Scratch scratch = SCRATCH.get();
        double[] cacheX = scratch.cacheX;
        double[] cacheY = scratch.cacheY;
        double[] cacheZ = scratch.cacheZ;
        noiseX.getGrid(MIN_Y, cz, SPAN, SIZE, sv, sh, cacheX);
        noiseY.getGrid(cx, cz, SIZE, SIZE, sh, sh, cacheY);
        noiseZ.getGrid(cx, MIN_Y, SIZE, SPAN, sh, sv, cacheZ);

        BlockPos.MutableBlockPos pos = scratch.pos;

        for (int ox = 0; ox < SIZE; ox++) {
            int x = cx + ox;
            for (int oz = 0; oz < SIZE; oz++) {
                int z = cz + oz;
                double ny = cacheY[ox * SIZE + oz];

                for (int y = MIN_Y; y <= MAX_Y; y++) {
                    int oy = y - MIN_Y;
                    double nx = cacheX[oy * SIZE + oz];
                    double nz = cacheZ[ox * SPAN + oy];
                    if (nx * ny * nz <= threshold) continue;
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
        private final double[] cacheX = new double[SPAN * SIZE];
        private final double[] cacheY = new double[SIZE * SIZE];
        private final double[] cacheZ = new double[SIZE * SPAN];
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    }
}
