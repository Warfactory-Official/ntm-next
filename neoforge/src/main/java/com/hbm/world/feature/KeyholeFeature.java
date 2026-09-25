// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class KeyholeFeature extends Feature<NoneFeatureConfiguration> {
    public KeyholeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {

        var random = context.random();
        if (random.nextInt(4) != 0) return false;

        int x = (context.origin().getX() & ~15) + random.nextInt(16);
        int y = 6 + random.nextInt(13);
        int z = (context.origin().getZ() & ~15) + random.nextInt(16);
        BlockPos pos = new BlockPos(x, y, z);
        if (!context.level().getBlockState(pos).is(Blocks.STONE)) return false;
        return context.level().setBlock(pos, ModBlocks.STONE_KEYHOLE.get().defaultBlockState(), 2);
    }
}
