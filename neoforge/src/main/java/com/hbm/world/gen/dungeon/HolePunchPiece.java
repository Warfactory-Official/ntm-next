// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.dungeon;

import com.hbm.lib.Library;
import com.hbm.world.structure.HbmStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class HolePunchPiece extends StructurePiece {

    public HolePunchPiece(BlockPos roomOrigin) {
        super(HbmStructureTypes.JUNGLE_HOLE_PUNCH.get(), 0, boxFor(roomOrigin));
    }

    public HolePunchPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.JUNGLE_HOLE_PUNCH.get(), tag);
    }

    private static BoundingBox boxFor(BlockPos roomOrigin) {
        int lo = JungleDungeonStructure.ROOM_WIDTH / 2 - 1;
        return new BoundingBox(
                roomOrigin.getX() + lo,
                roomOrigin.getY(),
                roomOrigin.getZ() + lo,
                roomOrigin.getX() + lo + 2,
                roomOrigin.getY(),
                roomOrigin.getZ() + lo + 2);
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
            BlockPos referencePos) {
        BlockState fragile =
                BuiltInRegistries.BLOCK
                        .getOptional(Library.id("brick_jungle_fragile"))
                        .map(Block::defaultBlockState)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "jungle dungeon hole punch needs hbm:brick_jungle_fragile"));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); x++) {
            for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); z++) {
                pos.set(x, this.boundingBox.minY(), z);
                if (chunkBB.isInside(pos)) level.setBlock(pos, fragile, 2);
            }
        }
    }
}
