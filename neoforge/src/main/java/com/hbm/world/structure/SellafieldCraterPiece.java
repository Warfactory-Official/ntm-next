// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class SellafieldCraterPiece extends StructurePiece {

    private final int originX;
    private final int originZ;
    private final int radius;

    public SellafieldCraterPiece(BlockPos origin, int radius, int minY, int maxY) {
        super(
                HbmStructureTypes.SELLAFIELD_CRATER_PIECE.get(),
                0,
                new BoundingBox(
                        origin.getX() - radius - 3,
                        minY,
                        origin.getZ() - radius - 3,
                        origin.getX() + radius + 3,
                        maxY,
                        origin.getZ() + radius + 3));

        this.setOrientation(null);
        this.originX = origin.getX();
        this.originZ = origin.getZ();
        this.radius = radius;
    }

    public SellafieldCraterPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.SELLAFIELD_CRATER_PIECE.get(), tag);
        this.originX = tag.getIntOr("OX", 0);
        this.originZ = tag.getIntOr("OZ", 0);
        this.radius = tag.getIntOr("CraterRadius", 8);
    }

    private static double depthFunc(double r, double radius, double depth) {
        return -(r * r) / (radius * radius) * depth + depth;
    }

    private static int findSurface(WorldGenLevel level, int x, int z, int minBuild) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getMaxY() - 1, z);
        while (pos.getY() > minBuild) {
            if (Heightmap.Types.MOTION_BLOCKING.isOpaque().test(level.getBlockState(pos))) {
                return pos.getY();
            }
            pos.move(0, -1, 0);
        }
        return minBuild;
    }

    private static BlockState sellafieldSlaked() {
        return BuiltInRegistries.BLOCK
                .getOptional(Library.id("sellafield_slaked"))
                .map(block -> block.defaultBlockState())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Sellafield crater needs hbm:sellafield_slaked, which is not registered"));
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", originX);
        tag.putInt("OZ", originZ);
        tag.putInt("CraterRadius", radius);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos) {
        BlockState fill = sellafieldSlaked();
        double depth = radius * 0.35D;
        int minBuild = level.getMinY();

        int minX = Math.max(chunkBB.minX(), chunkPos.getMinBlockX());
        int maxX = Math.min(chunkBB.maxX(), chunkPos.getMaxBlockX());
        int minZ = Math.max(chunkBB.minZ(), chunkPos.getMinBlockZ());
        int maxZ = Math.min(chunkBB.maxZ(), chunkPos.getMaxBlockZ());

        for (int x = minX; x <= maxX; x++) {
            int dx = x - originX;
            for (int z = minZ; z <= maxZ; z++) {
                int dz = z - originZ;
                double r = Math.sqrt((double) dx * dx + (double) dz * dz);

                if (r - random.nextInt(3) > radius) continue;

                int surfaceY = findSurface(level, x, z, minBuild);
                if (surfaceY <= minBuild) continue;

                int dep = (int) Mth.clamp(depthFunc(r, radius, depth), 0, surfaceY - minBuild - 1);
                for (int i = 0; i < dep; i++) {
                    this.placeBlock(
                            level, Blocks.AIR.defaultBlockState(), x, surfaceY - i, z, chunkBB);
                }

                int floorY = surfaceY - dep;
                int fillLayers = Math.max(0, Math.min(3, floorY - minBuild - 1));

                random.nextInt(3);
                for (int i = 0; i < fillLayers; i++) {
                    this.placeBlock(level, fill, x, floorY - i, z, chunkBB);
                }
            }
        }
    }
}
