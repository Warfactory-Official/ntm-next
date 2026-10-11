// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.lib.Library;
import com.hbm.world.LocationIsValidSpawn;
import com.hbm.world.gen.WorldgenHeight;
import com.hbm.world.gen.component.NtmComponentPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NtmProceduralPiece extends NtmComponentPiece {

    public static final Identifier TEMPLATE_ID = Library.id("component/antenna");

    static final int FOOTPRINT = 2;
    static final int TOP = 20;

    public NtmProceduralPiece(BlockPos origin) {
        super(
                HbmStructureTypes.NTM_PROCEDURAL_PIECE.get(),
                0,
                new BoundingBox(
                        origin.getX(),
                        origin.getY(),
                        origin.getZ(),
                        origin.getX() + FOOTPRINT,
                        origin.getY() + TOP,
                        origin.getZ() + FOOTPRINT));
        this.setOrientation(null);
    }

    public NtmProceduralPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_PROCEDURAL_PIECE.get(), context, tag);
    }

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
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
        int x = boundingBox.minX();
        int z = boundingBox.minZ();
        int y = WorldgenHeight.lightBlocking(level, x, z);
        if (!level.getBlockState(new BlockPos(x + 1, y, z + 1)).isAir()) return;
        if (!LocationIsValidSpawn.isValidGround(
                level.getBlockState(new BlockPos(x + 1, y - 1, z + 1)),
                level.getBlockState(new BlockPos(x + 1, y - 2, z + 1)),
                NtmProceduralPiece::isGround)) return;
        this.move(0, y - boundingBox.minY(), 0);
        super.postProcess(
                level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
    }

    private static boolean isGround(BlockState state) {
        return LocationIsValidSpawn.isGrass(state)
                || LocationIsValidSpawn.isDirtFamily(state)
                || LocationIsValidSpawn.isStone(state)
                || LocationIsValidSpawn.isSandFamily(state);
    }
}
