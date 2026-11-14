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

package com.hbm.world.gen.nbt;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.LootBlock.BlockEntityLoot;
import com.hbm.itempool.LootPile;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class LootPileProcessor implements StructureProcessor {

    public static final MapCodec<LootPileProcessor> MAP_CODEC =
            MapCodec.unit(() -> LootPileProcessor.INSTANCE);
    public static final LootPileProcessor INSTANCE = new LootPileProcessor();
    public static final String POOL = "pool";

    private LootPileProcessor() {}

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        CompoundTag nbt = processedBlockInfo.nbt();
        if (nbt == null || !processedBlockInfo.state().is(ModBlocks.LOOT.get()))
            return processedBlockInfo;
        Optional<String> pool = nbt.getString(POOL);
        if (pool.isEmpty()) return processedBlockInfo;

        BlockPos pos = processedBlockInfo.pos();
        if (settings.getBoundingBox() != null && !settings.getBoundingBox().isInside(pos))
            return processedBlockInfo;
        CompoundTag rolled = nbt.copy();
        rolled.remove(POOL);
        if (!BlockEntityLoot.holdsEntries(rolled)) {
            rolled.merge(
                    BlockEntityLoot.tagOf(
                            LootPile.roll(
                                    pool.get(),
                                    ProcessorRandom.source(level, pos, ProcessorRandom.LOOT_PILE),
                                    ProcessorRandom.worldSeed(level)),
                            level.registryAccess()));
        }
        return new StructureTemplate.StructureBlockInfo(pos, processedBlockInfo.state(), rolled);
    }

    @Override
    public MapCodec<LootPileProcessor> codec() {
        return MAP_CODEC;
    }
}
