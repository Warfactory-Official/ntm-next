// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.world.NtmWorldgenFields;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
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
        place(
                ctx.level(),
                ctx.random(),
                ctx.config(),
                ctx.origin().getX() & ~15,
                ctx.origin().getZ() & ~15);
        return true;
    }

    public static void place(
            WorldGenLevel level, RandomSource rand, Stratum3DConfig cfg, int x0, int z0) {
        NtmWorldgenFields fields = NtmWorldgenFields.get(level);
        double sh = cfg.scaleH();
        double sv = cfg.scaleV();
        double threshold = cfg.threshold();
        Stratum3DRows.Row rowX = rowX(fields, cfg.domainX(), z0, sv, sh);
        Stratum3DRows.Row rowZ = rowZ(fields, cfg.domainZ(), x0, sh, sv);
        Scratch scratch = SCRATCH.get();
        double[] cacheY = scratch.cacheY;
        fields.fractal(cfg.domainY(), 4).getGrid(x0, z0, SIZE, SIZE, sh, sh, cacheY);
        double[] cacheX = rowX.values();
        double[] cacheZ = rowZ.values();

        BlockPos.MutableBlockPos pos = scratch.pos;

        for (int ox = 0; ox < SIZE; ox++) {
            int x = x0 + ox;
            for (int oz = 0; oz < SIZE; oz++) {
                int z = z0 + oz;
                double ny = cacheY[ox * SIZE + oz];
                if (rowX.maxAbs()[oz] * Math.abs(ny) * rowZ.maxAbs()[ox] <= threshold) continue;

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
    }

    private static Stratum3DRows.Row rowX(
            NtmWorldgenFields fields, Identifier domain, int z0, double sv, double sh) {
        Stratum3DRows rows = fields.stratumRows(domain, sv, sh);
        Stratum3DRows.Row row = rows.get(z0);
        if (row != null) return row;
        double[] values = new double[SPAN * SIZE];
        fields.fractal(domain, 4).getGrid(MIN_Y, z0, SPAN, SIZE, sv, sh, values);
        double[] maxAbs = new double[SIZE];
        for (int oy = 0; oy < SPAN; oy++) {
            for (int oz = 0; oz < SIZE; oz++)
                maxAbs[oz] = Math.max(maxAbs[oz], Math.abs(values[oy * SIZE + oz]));
        }
        row = new Stratum3DRows.Row(z0, values, maxAbs);
        rows.put(row);
        return row;
    }

    private static Stratum3DRows.Row rowZ(
            NtmWorldgenFields fields, Identifier domain, int x0, double sh, double sv) {
        Stratum3DRows rows = fields.stratumRows(domain, sh, sv);
        Stratum3DRows.Row row = rows.get(x0);
        if (row != null) return row;
        double[] values = new double[SIZE * SPAN];
        fields.fractal(domain, 4).getGrid(x0, MIN_Y, SIZE, SPAN, sh, sv, values);
        double[] maxAbs = new double[SIZE];
        for (int ox = 0; ox < SIZE; ox++) {
            for (int oy = 0; oy < SPAN; oy++)
                maxAbs[ox] = Math.max(maxAbs[ox], Math.abs(values[ox * SPAN + oy]));
        }
        row = new Stratum3DRows.Row(x0, values, maxAbs);
        rows.put(row);
        return row;
    }

    private static final class Scratch {
        private final double[] cacheY = new double[SIZE * SIZE];
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    }
}
