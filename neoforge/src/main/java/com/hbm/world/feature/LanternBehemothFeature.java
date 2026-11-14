/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.LanternBehemothBlock;
import com.hbm.itempool.LootPile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
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
        if (!Block.canSupportCenter(level, pos.below(), Direction.UP)
                || !isReplaceable(level.getBlockState(pos))) return false;

        LanternBehemothBlock block = ModBlocks.LANTERN_BEHEMOTH.get();
        BlockState state =
                block.defaultBlockState()
                        .setValue(LanternBehemothBlock.FACING, Direction.NORTH)
                        .setValue(LanternBehemothBlock.BROKEN, true);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        block.fillSpace(level, pos, Direction.NORTH);

        if (context.random().nextInt(2) == 0) {
            BlockPos loot = pos.north(2);
            LootPile.BOOKLET.place(level, loot, context.random(), level.getSeed());
        }
        return true;
    }

    public static boolean isReplaceable(BlockState state) {
        return state.canBeReplaced() && !(state.getBlock() instanceof DoublePlantBlock);
    }
}
