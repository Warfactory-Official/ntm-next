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
import com.hbm.blocks.generic.BobbleBlock.Bobble;
import com.hbm.blocks.generic.BobbleBlock;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class BobbleRandomizerProcessor implements StructureProcessor {

    public static final MapCodec<BobbleRandomizerProcessor> MAP_CODEC =
            MapCodec.unit(() -> BobbleRandomizerProcessor.INSTANCE);
    public static final BobbleRandomizerProcessor INSTANCE = new BobbleRandomizerProcessor();

    private BobbleRandomizerProcessor() {}

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        if (!processedBlockInfo.state().is(ModBlocks.BOBBLEHEAD.get())) return processedBlockInfo;

        CompoundTag nbt =
                processedBlockInfo.nbt() == null
                        ? new CompoundTag()
                        : processedBlockInfo.nbt().copy();
        nbt.putString("id", "hbm:bobblehead");
        List<Bobble> types = BobbleBlock.TYPES.values();
        nbt.putString(
                "type",
                types.get(
                                ProcessorRandom.bounded(
                                                level,
                                                processedBlockInfo.pos(),
                                                ProcessorRandom.BOBBLE_TYPE,
                                                types.size() - 1)
                                        + 1)
                        .id());
        return new StructureTemplate.StructureBlockInfo(
                processedBlockInfo.pos(), processedBlockInfo.state(), nbt);
    }

    @Override
    public MapCodec<BobbleRandomizerProcessor> codec() {
        return MAP_CODEC;
    }
}
