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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class LockPinProcessor implements StructureProcessor {

    public static final MapCodec<LockPinProcessor> MAP_CODEC =
            MapCodec.unit(() -> LockPinProcessor.INSTANCE);
    public static final LockPinProcessor INSTANCE = new LockPinProcessor();

    private LockPinProcessor() {}

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        CompoundTag nbt = processedBlockInfo.nbt();
        if (nbt == null || !nbt.getBooleanOr("isLocked", false)) return processedBlockInfo;

        CompoundTag stamped = nbt.copy();
        stamped.putInt(
                "lock",
                ProcessorRandom.bounded(
                                level, processedBlockInfo.pos(), ProcessorRandom.LOCK_PINS, 999)
                        + 1);
        return new StructureTemplate.StructureBlockInfo(
                processedBlockInfo.pos(), processedBlockInfo.state(), stamped);
    }

    @Override
    public MapCodec<LockPinProcessor> codec() {
        return MAP_CODEC;
    }
}
