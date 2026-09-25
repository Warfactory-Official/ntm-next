// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.mojang.serialization.MapCodec;
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

        nbt.putByte(
                "type",
                (byte)
                        (ProcessorRandom.bounded(
                                        level,
                                        processedBlockInfo.pos(),
                                        ProcessorRandom.BOBBLE_TYPE,
                                        BobbleType.count() - 1)
                                + 1));
        return new StructureTemplate.StructureBlockInfo(
                processedBlockInfo.pos(), processedBlockInfo.state(), nbt);
    }

    @Override
    public MapCodec<BobbleRandomizerProcessor> codec() {
        return MAP_CODEC;
    }
}
