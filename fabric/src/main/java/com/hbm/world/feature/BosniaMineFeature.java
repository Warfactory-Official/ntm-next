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

import com.hbm.blockentity.bomb.LandmineBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.BalanceConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class BosniaMineFeature implements Feature {
    public static final MapCodec<BosniaMineFeature> CODEC = MapCodec.unit(BosniaMineFeature::new);

    @Override
    public MapCodec<BosniaMineFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos featureOrigin) {
        if (!BalanceConfig.enable528BosniaSimulator) return false;
        BlockPos pos = featureOrigin;
        if (!Block.canSupportCenter(level, pos.below(), Direction.UP)) return false;

        level.setBlock(pos, ModBlocks.MINE_HE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof LandmineBlockEntity mine)
            mine.deferUntilPlayerNear();
        return true;
    }
}
