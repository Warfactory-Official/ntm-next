/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.world.structure;

import com.hbm.world.feature.OilSurfaceScarring;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public class OilBubblePiece extends StructurePiece {
    private static final double H_EXTENT = 0.8165D;

    private final int centreX;
    private final int centreY;
    private final int centreZ;
    private final int radius;
    private final boolean fuzzy;
    private final BlockState core;
    private final RuleTest replaceable;
    private final long scarSeed;

    public OilBubblePiece(
            BlockPos centre,
            int radius,
            boolean fuzzy,
            BlockState core,
            RuleTest replaceable,
            long scarSeed) {
        super(HbmStructureTypes.OIL_BUBBLE_PIECE.get(), 0, box(centre, radius, scarSeed));
        this.setOrientation(null);
        this.centreX = centre.getX();
        this.centreY = centre.getY();
        this.centreZ = centre.getZ();
        this.radius = radius;
        this.fuzzy = fuzzy;
        this.core = core;
        this.replaceable = replaceable;
        this.scarSeed = scarSeed;
    }

    public OilBubblePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.OIL_BUBBLE_PIECE.get(), tag);
        this.centreX = tag.getIntOr("CX", 0);
        this.centreY = tag.getIntOr("CY", 0);
        this.centreZ = tag.getIntOr("CZ", 0);
        this.radius = tag.getIntOr("Radius", 8);
        this.fuzzy = tag.getBooleanOr("Fuzzy", false);
        this.core = tag.read("Core", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
        this.replaceable = tag.read("Replaceable", RuleTest.CODEC).orElse(null);
        this.scarSeed = tag.getLongOr("ScarSeed", 0L);
    }

    private static BoundingBox box(BlockPos centre, int radius, long scarSeed) {
        int h = Math.max((int) Math.ceil(radius * H_EXTENT), OilSurfaceScarring.reach(scarSeed));
        int v = (int) Math.ceil(radius * Math.sqrt(2.0D) / 3.0D);
        return new BoundingBox(
                centre.getX() - h,
                centre.getY() - v,
                centre.getZ() - h,
                centre.getX() + h,
                centre.getY() + v,
                centre.getZ() + h);
    }

    private static double jitter(long seed, int x, int y, int z) {
        long h =
                seed
                        ^ (x * 0x9E3779B97F4A7C15L)
                        ^ (y * 0xC2B2AE3D27D4EB4FL)
                        ^ (z * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", centreX);
        tag.putInt("CY", centreY);
        tag.putInt("CZ", centreZ);
        tag.putInt("Radius", radius);
        tag.putBoolean("Fuzzy", fuzzy);
        tag.store("Core", BlockState.CODEC, core);
        if (replaceable != null) tag.store("Replaceable", RuleTest.CODEC, replaceable);
        tag.putLong("ScarSeed", scarSeed);
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
        if (replaceable == null) return;

        double radiusSqr = (double) (radius * radius) / 2.0D;
        int h = (int) Math.ceil(radius * H_EXTENT);
        int v = (int) Math.ceil(radius * Math.sqrt(2.0D) / 3.0D);

        int minX = Math.max(chunkBB.minX(), centreX - h);
        int maxX = Math.min(chunkBB.maxX(), centreX + h);
        int minZ = Math.max(chunkBB.minZ(), centreZ - h);
        int maxZ = Math.min(chunkBB.maxZ(), centreZ + h);
        int minY = Math.max(Math.max(chunkBB.minY(), level.getMinY()), centreY - v);
        int maxY = Math.min(Math.min(chunkBB.maxY(), level.getMaxY()), centreY + v);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            int dxx = (x - centreX) * (x - centreX);
            for (int y = minY; y <= maxY; y++) {
                int dyy = dxx + (y - centreY) * (y - centreY) * 3;
                for (int z = minZ; z <= maxZ; z++) {
                    double dist = dyy + (double) (z - centreZ) * (z - centreZ);

                    double threshold = radiusSqr;
                    if (fuzzy) threshold += jitter(scarSeed, x, y, z) * radiusSqr / 3.0D;
                    if (dist >= threshold) continue;

                    pos.set(x, y, z);
                    if (replaceable.test(level.getBlockState(pos), (BlockPos) pos, random)) {
                        level.setBlock(pos, core, 2);
                    }
                }
            }
        }

        OilSurfaceScarring.addSurfaceSpot(
                level, RandomSource.create(scarSeed), centreX, centreZ, chunkBB);
    }

    public int scarX() {
        return centreX;
    }

    public int scarZ() {
        return centreZ;
    }

    public void projectHole(ProtoChunk chunk) {
        if (!core.is(BlockTags.BLOCKS_MOTION))
            throw new IllegalStateException("Surface projection requires a solid bubble core");
        OilSurfaceScarring.cutHoleColumn(
                chunk,
                (pos, state) -> chunk.setBlockState(pos, state),
                centreX,
                centreZ,
                chunk.getMaxY(),
                chunk.getMinY(),
                false);
    }
}
