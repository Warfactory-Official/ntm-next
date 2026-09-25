// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockCrashedBomb.EnumDudType;
import com.hbm.world.LocationIsValidSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class DudFeature extends Feature<NoneFeatureConfiguration> {

    private static final EnumDudType[] DUD_TYPES = EnumDudType.values();

    public DudFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos pos = ctx.origin();

        if (!level.getBlockState(pos).isAir()
                || !LocationIsValidSpawn.isValidGround(
                        level.getBlockState(pos.below()), level.getBlockState(pos.below(2)), false))
            return false;

        EnumDudType type = DUD_TYPES[rand.nextInt(DUD_TYPES.length)];
        level.setBlock(
                pos, ModBlocks.CRASHED_BOMBS.get(type.ordinal()).get().defaultBlockState(), 3);
        return true;
    }
}
