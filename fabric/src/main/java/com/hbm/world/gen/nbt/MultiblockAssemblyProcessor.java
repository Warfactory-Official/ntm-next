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

import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

public class MultiblockAssemblyProcessor implements StructureProcessor {

    public static final MapCodec<MultiblockAssemblyProcessor> MAP_CODEC =
            MapCodec.unit(() -> MultiblockAssemblyProcessor.INSTANCE);
    public static final MultiblockAssemblyProcessor INSTANCE = new MultiblockAssemblyProcessor();

    private MultiblockAssemblyProcessor() {}

    @Override
    public boolean evaluatesEntirePieceState() {
        return true;
    }

    private static void assembleFolded(
            ServerLevelAccessor level,
            BlockPos pos,
            BlockState coreState,
            MultiblockCoreBlock block,
            @Nullable BoundingBox clip) {
        if (clip == null || clip.isInside(pos)) level.setBlock(pos, coreState, 3);
        block.fillSpace(level, pos, coreState.getValue(MultiblockCoreBlock.FACING), clip);
        block.assembled(level, pos, coreState, clip);
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,
            BlockPos position,
            BlockPos referencePos,
            List<StructureTemplate.StructureBlockInfo> originalBlockInfoList,
            List<StructureTemplate.StructureBlockInfo> processedBlockInfoList,
            StructurePlaceSettings settings) {
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfoList) {
            if (info.state().getBlock() instanceof MultiblockCoreBlock folded) {
                assembleFolded(
                        level,
                        info.pos(),
                        info.state().mirror(settings.getMirror()).rotate(settings.getRotation()),
                        folded,
                        settings.getBoundingBox());
            }
        }
        return processedBlockInfoList;
    }

    @Override
    public MapCodec<MultiblockAssemblyProcessor> codec() {
        return MAP_CODEC;
    }
}
