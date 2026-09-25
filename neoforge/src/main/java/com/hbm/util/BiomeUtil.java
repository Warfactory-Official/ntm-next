// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
                (qx, qy, qz, sampler) -> {
                    Holder<Biome> existing = chunk.getNoiseBiome(qx, qy, qz);
                    int existingTier =
                            existing.is(innerKey)
                                    ? 3
                                    : existing.is(craterKey) ? 2 : existing.is(outerKey) ? 1 : 0;

                    double bx = QuartPos.toBlock(qx) + 2.0D;
                    double bz = QuartPos.toBlock(qz) + 2.0D;
                    double dist = Math.hypot(bx - centerX, bz - centerZ);
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

                    if (targetTier <= existingTier) return existing;
                    changed[0] = true;
                    return target;
                },
                level.getChunkSource().randomState().sampler());

        if (!changed[0]) return;
        chunk.markUnsaved();
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
    }
}
