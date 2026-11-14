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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public record PickOneProcessor(BlockState marker, BlockState result, List<BlockState> fallback)
        implements StructureProcessor {

    public static final MapCodec<PickOneProcessor> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            BlockState.CODEC
                                                    .fieldOf("marker")
                                                    .forGetter(PickOneProcessor::marker),
                                            BlockState.CODEC
                                                    .fieldOf("result")
                                                    .forGetter(PickOneProcessor::result),
                                            BlockState.CODEC
                                                    .listOf()
                                                    .fieldOf("fallback")
                                                    .forGetter(PickOneProcessor::fallback))
                                    .apply(i, PickOneProcessor::new));

    @Override
    public boolean evaluatesEntirePieceState() {
        return true;
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,
            BlockPos position,
            BlockPos referencePos,
            List<StructureTemplate.StructureBlockInfo> originalBlockInfoList,
            List<StructureTemplate.StructureBlockInfo> processedBlockInfoList,
            StructurePlaceSettings settings) {

        int candidateCount = 0;
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfoList) {
            if (info.state().equals(marker)) candidateCount++;
        }
        if (candidateCount == 0) return processedBlockInfoList;

        int chosen =
                ProcessorRandom.bounded(
                        level, position, ProcessorRandom.PICK_ONE, 0, candidateCount);

        List<StructureTemplate.StructureBlockInfo> out = new ArrayList<>(processedBlockInfoList);
        int candidate = 0;
        for (int index = 0; index < out.size(); index++) {
            StructureTemplate.StructureBlockInfo info = out.get(index);
            if (!info.state().equals(marker)) continue;
            BlockState replacement =
                    candidate++ == chosen
                            ? result
                            : fallback.isEmpty()
                                    ? info.state()
                                    : fallback.get(
                                            ProcessorRandom.bounded(
                                                    level,
                                                    info.pos(),
                                                    ProcessorRandom.PICK_FALLBACK,
                                                    fallback.size()));
            if (!replacement.equals(info.state())) {
                out.set(
                        index,
                        new StructureTemplate.StructureBlockInfo(
                                info.pos(), replacement, info.nbt()));
            }
        }
        return out;
    }

    @Override
    public MapCodec<PickOneProcessor> codec() {
        return MAP_CODEC;
    }
}
