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

import com.hbm.world.gen.WorldgenHeight;
import com.hbm.world.gen.component.*;
import com.hbm.world.gen.component.*;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class NtmFeaturesStructure extends Structure implements ColumnBiome.SelfTested {

    public static final MapCodec<NtmFeaturesStructure> CODEC =
            simpleCodec(NtmFeaturesStructure::new);

    private static final int FLATNESS_SAMPLE_RADIUS = 21;
    private static final int FLATNESS_TOLERANCE_BLOCKS = 4;

    public NtmFeaturesStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);
        RandomSource random = context.random();

        int centerHeight =
                context.chunkGenerator()
                        .getFirstOccupiedHeight(
                                x,
                                z,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState());
        if (!ColumnBiome.valid(context, x, centerHeight + 1, z)) return Optional.empty();
        int meanHeight =
                getMeanFirstOccupiedHeight(
                        context,
                        x - FLATNESS_SAMPLE_RADIUS,
                        FLATNESS_SAMPLE_RADIUS * 2,
                        z - FLATNESS_SAMPLE_RADIUS,
                        FLATNESS_SAMPLE_RADIUS * 2);
        boolean flat = Math.abs(meanHeight - centerHeight) <= FLATNESS_TOLERANCE_BLOCKS;

        Holder<Biome> biomeHolder =
                context.biomeResolver()
                        .getNoiseBiome(
                                QuartPos.fromBlock(x),
                                QuartPos.fromBlock(centerHeight),
                                QuartPos.fromBlock(z));
        Biome biome = biomeHolder.value();
        boolean hotDryNonMesa =
                biome.getBaseTemperature() >= 1.0F
                        && !biome.hasPrecipitation()
                        && !biomeHolder.is(BiomeTags.IS_BADLANDS);

        BlockPos origin = new BlockPos(x, centerHeight, z);

        if (flat && random.nextInt(10) == 0) {
            return Optional.of(
                    new Structure.GenerationStub(
                            origin,
                            builder ->
                                    builder.addPiece(SiloComponentPiece.create(context, origin))));
        }
        if (hotDryNonMesa) {
            NtmComponentPiece piece =
                    random.nextBoolean()
                            ? new NTMHouse1Piece(origin, random)
                            : new NTMHouse2Piece(origin, random);
            return grounded(context, origin, piece);
        }
        NtmComponentPiece piece =
                switch (random.nextInt(6)) {
                    case 0 -> new NTMLab2Piece(origin, random);
                    case 1 -> new NTMLab1Piece(origin, random);
                    case 2 -> new LargeOfficePiece(origin, random);
                    case 3 -> new LargeOfficeCornerPiece(origin, random);
                    default -> new RuralHouse1Piece(origin, random);
                };
        return grounded(context, origin, piece);
    }

    private static Optional<GenerationStub> grounded(
            GenerationContext context, BlockPos origin, NtmComponentPiece piece) {
        int surface = WorldgenHeight.averageGround(context, piece.getBoundingBox());
        piece.move(0, surface - origin.getY(), 0);
        return Optional.of(
                new GenerationStub(
                        new BlockPos(origin.getX(), surface, origin.getZ()),
                        builder -> builder.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.NTM_FEATURES.get();
    }
}
