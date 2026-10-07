// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.blocks.network.ChunkDuctNodes;
import com.hbm.blocks.network.FluidDuctBlockBase;
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
        if (!(processedBlockInfo.state().getBlock() instanceof FluidDuctBlockBase)) {
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
