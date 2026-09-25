// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.world.structure.HbmStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NullSpawnPiece extends StructurePiece {

    public NullSpawnPiece(BlockPos origin) {
        super(
                HbmStructureTypes.NULL_SPAWN_PIECE.get(),
                0,
                new BoundingBox(
                        origin.getX(),
                        origin.getY(),
                        origin.getZ(),
                        origin.getX(),
                        origin.getY(),
                        origin.getZ()));
    }

    public NullSpawnPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NULL_SPAWN_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos) {}
}
