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

package com.hbm.blocks.bomb;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.FissureBlock;
import com.hbm.entity.item.TntNtmEntity;
import com.hbm.explosion.MiniNukeExplosion;
import com.hbm.util.BiomeUtil;
import com.hbm.world.biome.NtmBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.jspecify.annotations.Nullable;

public class FissureBombBlock extends BaseTNTBlock {

    public FissureBombBlock(Properties properties) {
        super(properties, 20);
    }

    @Override
    public void explodeEntity(
            Level level, double x, double y, double z, @Nullable TntNtmEntity entity) {
        MiniNukeExplosion.MEDIUM.detonate(level, x, y, z);

        var center = BlockPos.containing(x, y, z);
        boolean crater = NtmBiomes.isCrater(BiomeUtil.biomeAt((ServerLevel) level, center));
        var fissure =
                ModBlocks.ORE_VOLCANO
                        .get()
                        .defaultBlockState()
                        .setValue(FissureBlock.CRATER, crater);
        var ore = ModBlocks.ORE_BEDROCK.get();
        var oilOre = ModBlocks.ORE_BEDROCK_OIL.get();
        var bedrock = Blocks.BEDROCK.defaultBlockState();
        var cursor = new BlockPos.MutableBlockPos();

        for (int offsetX = -5; offsetX <= 5; offsetX++) {
            for (int offsetY = -5; offsetY <= 5; offsetY++) {
                for (int offsetZ = -5; offsetZ <= 5; offsetZ++) {
                    cursor.set(
                            center.getX() + offsetX,
                            center.getY() + offsetY,
                            center.getZ() + offsetZ);
                    var state = level.getBlockState(cursor);
                    if (state.is(ore)) {
                        level.setBlock(cursor, fissure, Block.UPDATE_ALL);
                    } else if (state.is(oilOre)) {
                        level.setBlock(cursor, bedrock, Block.UPDATE_ALL);
                    }
                }
            }
        }
    }
}
