// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
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
            BlockMultiblockCore block,
            @Nullable BoundingBox clip) {
        if (clip == null || clip.isInside(pos)) level.setBlock(pos, coreState, 3);
        block.fillSpace(level, pos, coreState.getValue(BlockMultiblockCore.FACING), clip);
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
            if (info.state().getBlock() instanceof BlockMultiblockCore folded) {
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
