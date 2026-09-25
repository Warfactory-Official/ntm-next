// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class OilBubbleFeature extends Feature<OilBubbleConfig> {

    public OilBubbleFeature() {
        super(OilBubbleConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<OilBubbleConfig> ctx) {
        WorldGenLevel level = ctx.level();
        OilBubbleConfig cfg = ctx.config();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();

        int radius = cfg.minSize() + rand.nextInt(cfg.maxSize() - cfg.minSize());

        double radiusSqr = (radius * radius) / 2.0D;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx < radius; dx++) {
            int xx = dx * dx;
            for (int dy = -radius; dy < radius; dy++) {
                int yy = xx + dy * dy * 3;
                for (int dz = -radius; dz < radius; dz++) {
                    int zz = yy + dz * dz;
                    double threshold = radiusSqr;
                    if (cfg.fuzzy()) threshold += rand.nextDouble() * radiusSqr / 3.0D;
                    if (zz >= threshold) continue;

                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (cfg.replaceable().test(level.getBlockState(pos), rand)) {
                        level.setBlock(pos, cfg.core(), 2);
                    }
                }
            }
        }

        OilSurfaceScarring.addSurfaceSpot(level, rand, origin.getX(), origin.getZ());
        return true;
    }
}
