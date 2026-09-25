// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockLanternBehemoth;
import com.hbm.blocks.generic.BlockLoot;
import com.hbm.util.LootGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class LanternBehemothFeature extends Feature<NoneFeatureConfiguration> {

    public LanternBehemothFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        BlockPos pos = context.origin();
        if (!LockedSafeFeature.canPlaceTorchOnTop(level.getBlockState(pos.below()))
                || !level.getBlockState(pos).canBeReplaced()) return false;

        BlockLanternBehemoth block = ModBlocks.LANTERN_BEHEMOTH.get();
        BlockState state =
                block.defaultBlockState()
                        .setValue(BlockLanternBehemoth.FACING, Direction.NORTH)
                        .setValue(BlockLanternBehemoth.BROKEN, true);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        block.fillSpace(level, pos, Direction.NORTH);

        if (context.random().nextInt(2) == 0) {
            BlockPos loot = pos.north(2);
            BlockLoot.place(
                    level,
                    loot,
                    LootGenerator.roll(
                            LootGenerator.LOOT_BOOKLET, context.random(), level.getSeed()));
        }
        return true;
    }
}
