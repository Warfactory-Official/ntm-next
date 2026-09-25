// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class NetherSurfaceFeature extends Feature<NetherSurfaceConfig> {

    public NetherSurfaceFeature() {
        super(NetherSurfaceConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NetherSurfaceConfig> ctx) {
        WorldGenLevel level = ctx.level();
        NetherSurfaceConfig cfg = ctx.config();
        RandomSource rand = ctx.random();
        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int k = 0; k < cfg.attempts(); k++) {
            int x = cx + rand.nextInt(16);
            int z = cz + rand.nextInt(16);
            int d = cfg.minD() + rand.nextInt(cfg.dRange());
            for (int y = d - cfg.scanDown(); y <= d; y++) {
                pos.set(x, y + 1, z);
                if (level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.setY(y)).is(Blocks.NETHERRACK)) {
                    level.setBlock(pos, cfg.state(), 2);
                }
            }
        }
        return true;
    }
}
