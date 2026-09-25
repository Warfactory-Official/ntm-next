// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class DepthDepositFeature extends Feature<DepthDepositConfig> {

    public DepthDepositFeature() {
        super(DepthDepositConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<DepthDepositConfig> ctx) {
        WorldGenLevel level = ctx.level();
        DepthDepositConfig cfg = ctx.config();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();

        int size = cfg.size();
        double coreRadius = size * cfg.fill();

        WorldGenerationContext generation = new WorldGenerationContext(ctx.chunkGenerator(), level);
        int floor = generation.getMinGenY();
        int ceiling = floor + generation.getGenDepth() - 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -size; dx <= size; dx++) {
            for (int dy = -size; dy <= size; dy++) {
                for (int dz = -size; dz <= size; dz++) {
                    int y = origin.getY() + dy;

                    if (y <= floor || y >= ceiling) continue;

                    pos.set(origin.getX() + dx, y, origin.getZ() + dz);
                    BlockState cur = level.getBlockState(pos);
                    if (!cfg.replaceable().test(cur, rand) && !cur.is(Blocks.BEDROCK)) continue;

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
