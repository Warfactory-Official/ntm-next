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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

public record MaybePlaceProcessor(BlockState marker, BlockState result, float probability)
        implements MarkerProcessor {

    public static final MapCodec<MaybePlaceProcessor> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            BlockState.CODEC
                                                    .fieldOf("marker")
                                                    .forGetter(MaybePlaceProcessor::marker),
                                            BlockState.CODEC
                                                    .fieldOf("result")
                                                    .forGetter(MaybePlaceProcessor::result),
                                            Codec.floatRange(0.0F, 1.0F)
                                                    .fieldOf("probability")
                                                    .forGetter(MaybePlaceProcessor::probability))
                                    .apply(i, MaybePlaceProcessor::new));

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        if (!processedBlockInfo.state().equals(marker)) return processedBlockInfo;

        if (ProcessorRandom.unitFloat(level, processedBlockInfo.pos(), ProcessorRandom.MAYBE)
                > probability) {
            return null;
        }
        return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), result, null);
    }

    @Override
    public MapCodec<MaybePlaceProcessor> codec() {
        return MAP_CODEC;
    }
}
