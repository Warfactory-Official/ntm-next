// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockLoot.TileEntityLoot;
import com.hbm.util.LootGenerator;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class LootPileProcessor implements StructureProcessor {

    public static final MapCodec<LootPileProcessor> MAP_CODEC =
            MapCodec.unit(() -> LootPileProcessor.INSTANCE);
    public static final LootPileProcessor INSTANCE = new LootPileProcessor();
    public static final String POOL = "pool";

    private LootPileProcessor() {}

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        CompoundTag nbt = processedBlockInfo.nbt();
        if (nbt == null || !processedBlockInfo.state().is(ModBlocks.LOOT.get()))
            return processedBlockInfo;
        Optional<String> pool = nbt.getString(POOL);
        if (pool.isEmpty()) return processedBlockInfo;

        BlockPos pos = processedBlockInfo.pos();
        CompoundTag rolled = nbt.copy();
        rolled.remove(POOL);
        rolled.merge(
                TileEntityLoot.tagOf(
                        LootGenerator.roll(
                                pool.get(),
                                ProcessorRandom.source(level, pos, ProcessorRandom.LOOT_PILE),
                                ProcessorRandom.worldSeed(level)),
                        level.registryAccess()));
        return new StructureTemplate.StructureBlockInfo(pos, processedBlockInfo.state(), rolled);
    }

    @Override
    public MapCodec<LootPileProcessor> codec() {
        return MAP_CODEC;
    }
}
