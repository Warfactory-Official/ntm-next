// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class GroundHeightProcessor extends GravityProcessor {
    public static final MapCodec<GravityProcessor> MAP_CODEC =
            Codec.INT
                    .optionalFieldOf("offset", 0)
                    .xmap(
                            GroundHeightProcessor::new,
                            processor -> ((GroundHeightProcessor) processor).offset);
    private final int offset;

    public GroundHeightProcessor(int offset) {
        super(Heightmap.Types.OCEAN_FLOOR_WG, offset);
        this.offset = offset;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        BlockPos pos = processedBlockInfo.pos();
        BoundingBox box = settings.getBoundingBox();

        if (box != null
                && (pos.getX() < box.minX()
                        || pos.getX() > box.maxX()
                        || pos.getZ() < box.minZ()
                        || pos.getZ() > box.maxZ())) return processedBlockInfo;

        int y =
                WorldgenHeight.ground(level, pos.getX(), pos.getZ())
                        + offset
                        + templateRelativePos.getY();
        return new StructureTemplate.StructureBlockInfo(
                new BlockPos(pos.getX(), y, pos.getZ()),
                processedBlockInfo.state(),
                processedBlockInfo.nbt());
    }

    @Override
    public MapCodec<GravityProcessor> codec() {
        return MAP_CODEC;
    }
}
