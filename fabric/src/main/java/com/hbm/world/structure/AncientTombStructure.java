/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

import com.hbm.world.gen.LegacyWorldgenRotation;
import com.hbm.world.gen.component.AncientTombPiece;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class AncientTombStructure extends Structure implements ColumnBiome.SelfTested {

    public static final MapCodec<AncientTombStructure> CODEC =
            simpleCodec(AncientTombStructure::new);

    private static final double SPIKE_RADIUS = 20.0;

    public AncientTombStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);
        int height =
                context.chunkGenerator()
                        .getBaseHeight(
                                x,
                                z,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState());
        if (!ColumnBiome.valid(context, x, height, z)) return Optional.empty();
        int yOff = Math.max(height, 35) - 5;

        BlockPos origin = new BlockPos(x, yOff, z);
        return Optional.of(
                new Structure.GenerationStub(
                        origin, builder -> addPiece(context, builder, x, z, yOff)));
    }

    private static void addPiece(
            Structure.GenerationContext context,
            StructurePiecesBuilder builder,
            int x,
            int z,
            int yOff) {
        RandomSource random = context.random();
        int spikeCount = 36 + random.nextInt(15);
        int[] spikeX = new int[spikeCount];
        int[] spikeZ = new int[spikeCount];
        int[] spikeY = new int[spikeCount];
        int[] offsets = spikeOffsets(spikeCount, random, x, z);
        for (int i = 0; i < spikeCount; i++) {
            int ix = offsets[i * 2], iz = offsets[i * 2 + 1];
            int iy =
                    context.chunkGenerator()
                                    .getBaseHeight(
                                            ix,
                                            iz,
                                            Heightmap.Types.WORLD_SURFACE_WG,
                                            context.heightAccessor(),
                                            context.randomState())
                            - 3;
            spikeX[i] = ix;
            spikeZ[i] = iz;
            spikeY[i] = iy;
        }
        builder.addPiece(new AncientTombPiece(x, z, yOff, spikeX, spikeZ, spikeY));
    }

    public static int[] spikeOffsets(int count, RandomSource random, int x, int z) {
        int[] positions = new int[count * 2];
        float rot = (float) Math.toRadians(360F / count);
        float cosine = LegacyWorldgenRotation.cosine(rot), sine = LegacyWorldgenRotation.sine(rot);
        double vx = SPIKE_RADIUS, vz = 0;
        for (int i = 0; i < count; i++) {
            double nextX = vx * cosine + vz * sine;
            vz = vz * cosine - vx * sine;
            vx = nextX;
            double variance = 1.0 + random.nextDouble() * 0.4;
            positions[i * 2] = (int) (x + vx * variance);
            positions[i * 2 + 1] = (int) (z + vz * variance);
        }
        return positions;
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.ANCIENT_TOMB.get();
    }
}
