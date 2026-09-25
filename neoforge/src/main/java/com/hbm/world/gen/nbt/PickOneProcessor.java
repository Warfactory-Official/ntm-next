// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
