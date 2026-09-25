// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.saveddata.TomSaveData;
import com.hbm.world.gen.WorldgenHeight;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class FlowerPatchFeature extends Feature<NoneFeatureConfiguration> {

    private final Supplier<Block> flower;
    private final int odds;

    public FlowerPatchFeature(Supplier<Block> flower, int odds) {
        super(NoneFeatureConfiguration.CODEC);
        this.flower = flower;
        this.odds = odds;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (TomSaveData.impact(level.getLevel()) || random.nextInt(odds) != 0) return false;

        BlockPos chunkOrigin = context.origin();

        int x = (chunkOrigin.getX() & ~15) + random.nextInt(16);
        int z = (chunkOrigin.getZ() & ~15) + random.nextInt(16);
        int y = WorldgenHeight.lightBlocking(level, x, z);
        BlockState flower = this.flower.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int attempt = 0; attempt < 64; attempt++) {
            pos.set(
                    x + random.nextInt(8) - random.nextInt(8),
                    y + random.nextInt(4) - random.nextInt(4),
                    z + random.nextInt(8) - random.nextInt(8));
            if (level.isEmptyBlock(pos)
                    && (level.getLevel().dimensionType().hasSkyLight() || pos.getY() < 255)
                    && flower.canSurvive(level, pos)) {
                level.setBlock(pos, flower, 2);
                placed = true;
            }
        }
        return placed;
    }
}
