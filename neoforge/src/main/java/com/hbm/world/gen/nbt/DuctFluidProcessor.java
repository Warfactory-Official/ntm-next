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

import com.hbm.blocks.network.ChunkDuctNodes;
import com.hbm.blocks.network.FluidDuctBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class DuctFluidProcessor implements StructureProcessor {

    public static final String KEY = "hbm_duct_fluid";
    public static final MapCodec<DuctFluidProcessor> MAP_CODEC =
            MapCodec.unit(() -> DuctFluidProcessor.INSTANCE);
    public static final DuctFluidProcessor INSTANCE = new DuctFluidProcessor();

    private DuctFluidProcessor() {}

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        CompoundTag nbt = processedBlockInfo.nbt();
        if (nbt == null || !nbt.contains(KEY)) return processedBlockInfo;
        if (!(processedBlockInfo.state().getBlock() instanceof FluidDuctBlock)) {
            throw new IllegalStateException(KEY + " on " + processedBlockInfo.state());
        }
        BoundingBox box = settings.getBoundingBox();
        if (box == null || box.isInside(processedBlockInfo.pos())) {
            ChunkDuctNodes.record(
                    level.getChunk(processedBlockInfo.pos()),
                    processedBlockInfo.pos(),
                    Identifier.parse(nbt.getStringOr(KEY, "")));
        }
        return new StructureTemplate.StructureBlockInfo(
                processedBlockInfo.pos(), processedBlockInfo.state(), null);
    }

    @Override
    public MapCodec<DuctFluidProcessor> codec() {
        return MAP_CODEC;
    }
}
