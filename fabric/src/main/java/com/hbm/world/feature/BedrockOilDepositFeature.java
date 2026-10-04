// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class BedrockOilDepositFeature extends Feature<BedrockOilDepositConfig> {

    private static final int DXZ_LIMIT = 4;
    public static final int MAX_Y = 4;
    private static final int L1_MAX = 6;
    private static final int OIL_SPOT_WIDTH = 5;
    private static final int OIL_SPOT_COUNT = 50;

    public BedrockOilDepositFeature() {
        super(BedrockOilDepositConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<BedrockOilDepositConfig> ctx) {
        WorldGenLevel level = ctx.level();
        int centerX = ctx.origin().getX();
        int centerZ = ctx.origin().getZ();
        boolean placedAny = placeNode(level, ctx.config(), ctx.random(), centerX, centerZ);
        OilSurfaceScarring.addBedrockOilSpot(
                level, ctx.random(), centerX, centerZ, OIL_SPOT_WIDTH, OIL_SPOT_COUNT);
        return placedAny;
    }

    public static boolean placeNode(
            WorldGenLevel level,
            BedrockOilDepositConfig cfg,
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
                    if (!cfg.replaceable().test(cur, random) && !cur.is(Blocks.BEDROCK)) continue;
                    level.setBlock(pos, cfg.core(), 2);
                    placedAny = true;
                }
            }
        }
        return placedAny;
    }
}
