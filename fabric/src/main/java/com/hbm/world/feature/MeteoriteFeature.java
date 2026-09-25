// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class MeteoriteFeature extends Feature<NoneFeatureConfiguration> {

    public MeteoriteFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();

        BlockPos source = ctx.origin();
        BlockPos.MutableBlockPos origin =
                new BlockPos.MutableBlockPos(
                        source.getX(), source.getY() - rand.nextInt(10), source.getZ());

        BlockState below2 = level.getBlockState(origin.move(0, -2, 0));
        origin.move(0, 2, 0);
        if (below2.isAir() || below2.liquid() || origin.getY() <= 1) return false;

        Meteorite.INSTANCE.generate(level, rand, origin, false, false, false);
        return true;
    }
}
