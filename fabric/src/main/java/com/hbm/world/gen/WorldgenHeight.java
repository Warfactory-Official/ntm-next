/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.world.gen;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;

public final class WorldgenHeight {
    private WorldgenHeight() {}

    public static @Nullable ChunkAccess heldChunk(LevelReader level, int x, int z) {
        return level.getChunk(x >> 4, z >> 4, ChunkStatus.EMPTY, false);
    }

    public static int lightBlocking(LevelReader level, int x, int z) {
        return lightBlocking(heldChunk(level, x, z), x, z);
    }

    public static int lightBlocking(ChunkAccess chunk, int x, int z) {
        return scan(chunk, x, z, false);
    }

    public static int lightBlocking(ChunkAccess chunk, BlockPos.MutableBlockPos pos) {
        return scan(chunk, pos, false);
    }

    public static int ground(LevelReader level, int x, int z) {
        return scan(heldChunk(level, x, z), x, z, true);
    }

    public static int ground(ChunkAccess chunk, int x, int z) {
        return scan(chunk, x, z, true);
    }

    public static int highestFilledSectionTop(LevelReader level, int x, int z) {
        return highestFilledSectionTop(heldChunk(level, x, z));
    }

    public static int highestFilledSectionTop(ChunkAccess chunk) {
        if (chunk instanceof ImposterProtoChunk imposter) chunk = imposter.getWrapped();
        int highest = chunk.getHighestFilledSectionIndex();
        return highest == -1
                ? chunk.getMinY()
                : Math.min(
                        SectionPos.sectionToBlockCoord(
                                chunk.getSectionYFromSectionIndex(highest), 15),
                        chunk.getMaxY());
    }

    private static int scan(ChunkAccess chunk, int x, int z, boolean ground) {
        return scan(chunk, new BlockPos.MutableBlockPos(x, 0, z), ground);
    }

    private static int scan(ChunkAccess chunk, BlockPos.MutableBlockPos pos, boolean ground) {
        if (chunk instanceof ImposterProtoChunk imposter) chunk = imposter.getWrapped();
        int highest = chunk.getHighestFilledSectionIndex();
        int top =
                highest == -1
                        ? chunk.getMinY()
                        : Math.min(
                                SectionPos.sectionToBlockCoord(
                                        chunk.getSectionYFromSectionIndex(highest), 15),
                                chunk.getMaxY());
        for (int index = highest; index >= 0; index--) {
            var section = chunk.getSections()[index];
            if (section.hasOnlyAir()) continue;
            int bottom = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            int lowestY = Math.max(bottom, chunk.getMinY());
            for (int y = Math.min(top, bottom + 15); y >= lowestY; y--) {
                BlockState state = section.getBlockState(pos.getX() & 15, y & 15, pos.getZ() & 15);
                if (ground
                        ? (state.blocksMotion() && !state.is(BlockTags.LEAVES))
                        : state.getLightDampening() > 0) {
                    pos.setY(y);
                    return y + 1;
                }
            }
        }
        pos.setY(chunk.getMinY());
        return chunk.getMinY();
    }

    public static int averageGround(Structure.GenerationContext context, BoundingBox box) {
        return averageGround(context, box.minX(), box.minZ(), box.maxX(), box.maxZ());
    }

    public static int averageGround(
            Structure.GenerationContext context, int minX, int minZ, int maxX, int maxZ) {
        int xSamples = symmetricSampleCount(minX, maxX);
        int zSamples = symmetricSampleCount(minZ, maxZ);
        int total = 0;
        for (int zi = 0; zi < zSamples; zi++) {
            int z = symmetricSample(minZ, maxZ, zSamples, zi);
            for (int xi = 0; xi < xSamples; xi++) {
                int x = symmetricSample(minX, maxX, xSamples, xi);
                total +=
                        Math.max(
                                context.chunkGenerator()
                                        .getBaseHeight(
                                                x,
                                                z,
                                                Heightmap.Types.OCEAN_FLOOR_WG,
                                                context.heightAccessor(),
                                                context.randomState()),
                                64);
            }
        }
        return total / (xSamples * zSamples);
    }

    public static int averageBaseHeight(Structure.GenerationContext context, BoundingBox box) {
        long total = 0;
        for (int height :
                baseHeights(
                        context.chunkGenerator(),
                        context.heightAccessor(),
                        context.randomState(),
                        box)) {
            total += height;
        }
        return (int) (total / ((long) box.getXSpan() * box.getZSpan()));
    }

    public static int[] baseHeights(
            ChunkGenerator generator,
            LevelHeightAccessor heightAccessor,
            RandomState randomState,
            BoundingBox box) {
        int[] out = new int[box.getXSpan() * box.getZSpan()];
        if (generator.getClass() == NoiseBasedChunkGenerator.class
                && BaseHeightScan.fill(
                        (NoiseBasedChunkGenerator) generator,
                        heightAccessor,
                        randomState,
                        box,
                        out)) {
            return out;
        }
        int i = 0;
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                out[i++] =
                        generator.getBaseHeight(
                                x, z, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState);
            }
        }
        return out;
    }

    public static void fillUnstructuredTerrain(
            Structure.GenerationContext context, ProtoChunk chunk) {
        var generator = (NoiseBasedChunkGenerator) context.chunkGenerator();
        var settings = generator.generatorSettings().value();
        var noiseSettings =
                settings.noiseSettings()
                        .clampToHeightAccessor(chunk.getHeightAccessorForGeneration());
        var random = context.randomState();
        var source = generator.getBiomeSource();
        var biomes =
                new BiomeManager(
                        (x, y, z) -> source.getNoiseBiome(x, y, z, random.sampler()),
                        BiomeManager.obfuscateSeed(context.seed()));
        chunk.fillBiomesFromNoise(source, random.sampler());
        var noise =
                chunk.getOrCreateNoiseChunk(
                        c ->
                                new NoiseChunk(
                                        16 / noiseSettings.getCellWidth(),
                                        random,
                                        c.getPos().getMinBlockX(),
                                        c.getPos().getMinBlockZ(),
                                        noiseSettings,
                                        DensityFunctions.BeardifierMarker.INSTANCE,
                                        settings,
                                        generator.globalFluidPicker.get(),
                                        Blender.empty()));
        generator.doFill(
                Blender.empty(),
                null,
                random,
                chunk,
                Mth.floorDiv(noiseSettings.minY(), noiseSettings.getCellHeight()),
                Mth.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight()));
        if (!SharedConstants.DEBUG_DISABLE_SURFACE
                && !SharedConstants.debugVoidTerrain(chunk.getPos())) {
            generator.buildSurface(
                    chunk,
                    new WorldGenerationContext(generator, context.heightAccessor()),
                    random,
                    null,
                    biomes,
                    Blender.empty(),
                    null);
        }
        if (SharedConstants.DEBUG_DISABLE_CARVERS) return;
        var carving =
                new CarvingContext(
                        generator,
                        context.registryAccess(),
                        chunk.getHeightAccessorForGeneration(),
                        noise,
                        random,
                        settings.surfaceRule());
        var carverRandom = new WorldgenRandom(new LegacyRandomSource(0));
        ChunkPos at = chunk.getPos();
        for (int dx = -8; dx <= 8; dx++)
            for (int dz = -8; dz <= 8; dz++) {
                ChunkPos from = new ChunkPos(at.x() + dx, at.z() + dz);
                var biome =
                        source.getNoiseBiome(
                                QuartPos.fromBlock(from.getMinBlockX()),
                                0,
                                QuartPos.fromBlock(from.getMinBlockZ()),
                                random.sampler());
                int index = 0;
                for (var holder : generator.getBiomeGenerationSettings(biome).getCarvers()) {
                    var carver = holder.value();
                    carverRandom.setLargeFeatureSeed(context.seed() + index++, from.x(), from.z());
                    if (carver.isStartChunk(carverRandom)) {
                        carver.carve(
                                carving,
                                chunk,
                                biomes::getBiome,
                                carverRandom,
                                noise.aquifer(),
                                from,
                                chunk.getOrCreateCarvingMask());
                    }
                }
            }
    }

    private static int symmetricSampleCount(int min, int max) {
        if (min == max) return 1;
        int lowerMiddle = min + (max - min) / 2;
        int upperMiddle = max - (max - min) / 2;
        if (lowerMiddle == min && upperMiddle == max) return 2;
        return lowerMiddle == upperMiddle ? 3 : 4;
    }

    private static int symmetricSample(int min, int max, int count, int index) {
        if (index == 0) return min;
        if (index == count - 1) return max;
        return index == 1 ? min + (max - min) / 2 : max - (max - min) / 2;
    }
}
