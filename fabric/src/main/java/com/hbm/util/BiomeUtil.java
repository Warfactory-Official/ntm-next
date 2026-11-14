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

package com.hbm.util;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class BiomeUtil {

    private BiomeUtil() {}

    public static Holder<Biome> biomeAt(ServerLevel level, BlockPos pos) {
        return level.getChunkAt(pos)
                .getNoiseBiome(
                        QuartPos.fromBlock(pos.getX()),
                        QuartPos.fromBlock(pos.getY()),
                        QuartPos.fromBlock(pos.getZ()));
    }

    public static Holder<Biome> craterBiome(
            Holder<Biome> existing,
            int qx,
            int qz,
            double centerX,
            double centerZ,
            int scale,
            Holder<Biome> inner,
            Holder<Biome> crater,
            Holder<Biome> outer,
            ResourceKey<Biome> innerKey,
            ResourceKey<Biome> craterKey,
            ResourceKey<Biome> outerKey) {
        int existingTier =
                existing.is(innerKey)
                        ? 3
                        : existing.is(craterKey) ? 2 : existing.is(outerKey) ? 1 : 0;
        double bx = QuartPos.toBlock(qx) + 2.0D;
        double bz = QuartPos.toBlock(qz) + 2.0D;
        double dist = Math.hypot(bx - centerX, bz - centerZ);
        if (dist > scale) return existing;
        double percent = scale <= 0 ? 100.0D : dist * 100.0D / scale;
        int targetTier;
        Holder<Biome> target;
        if (scale >= 150 && percent < 15) {
            targetTier = 3;
            target = inner;
        } else if (scale >= 100 && percent < 55) {
            targetTier = 2;
            target = crater;
        } else if (scale >= 25) {
            targetTier = 1;
            target = outer;
        } else return existing;
        return targetTier <= existingTier ? existing : target;
    }

    public static void paintCraterBiome(
            ServerLevel level,
            ChunkPos chunkPos,
            double centerX,
            double centerZ,
            int scale,
            Holder<Biome> inner,
            Holder<Biome> crater,
            Holder<Biome> outer,
            ResourceKey<Biome> innerKey,
            ResourceKey<Biome> craterKey,
            ResourceKey<Biome> outerKey) {
        ChunkAccess chunk = level.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, false);
        if (chunk == null) return;

        boolean[] changed = {false};
        chunk.fillBiomesFromNoise(
                (qx, qy, qz) -> {
                    Holder<Biome> existing = chunk.getNoiseBiome(qx, qy, qz);
                    Holder<Biome> target =
                            craterBiome(
                                    existing, qx, qz, centerX, centerZ, scale, inner, crater, outer,
                                    innerKey, craterKey, outerKey);
                    changed[0] |= target != existing;
                    return target;
                });

        if (!changed[0]) return;
        chunk.markUnsaved();
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
    }
}
