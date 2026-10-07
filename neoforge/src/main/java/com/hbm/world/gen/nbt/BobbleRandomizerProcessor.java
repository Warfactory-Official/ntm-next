// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBobble.Bobble;
import com.hbm.blocks.generic.BlockBobble;
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
        List<Bobble> types = BlockBobble.TYPES.values();
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
