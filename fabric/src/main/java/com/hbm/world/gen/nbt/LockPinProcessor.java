// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
