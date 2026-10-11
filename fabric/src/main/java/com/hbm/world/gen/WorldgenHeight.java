// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public final class WorldgenHeight {
    private WorldgenHeight() {}

    public static int lightBlocking(LevelReader level, int x, int z) {
        return lightBlocking(level.getChunk(x >> 4, z >> 4, ChunkStatus.EMPTY, false), x, z);
    }

    public static int lightBlocking(ChunkAccess chunk, int x, int z) {
        return scan(chunk, x, z, false);
    }

    public static int ground(LevelReader level, int x, int z) {
        return scan(level.getChunk(x >> 4, z >> 4, ChunkStatus.EMPTY, false), x, z, true);
    }

    public static int ground(ChunkAccess chunk, int x, int z) {
        return scan(chunk, x, z, true);
    }

    public static int highestFilledSectionTop(LevelReader level, int x, int z) {
        ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.EMPTY, false);
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
        if (chunk instanceof ImposterProtoChunk imposter) chunk = imposter.getWrapped();
        int highest = chunk.getHighestFilledSectionIndex();
        int top =
                highest == -1
                        ? chunk.getMinY()
                        : Math.min(
                                SectionPos.sectionToBlockCoord(
                                        chunk.getSectionYFromSectionIndex(highest), 15),
                                chunk.getMaxY());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        for (int y = top; y >= chunk.getMinY(); y--) {
            BlockState state = chunk.getBlockState(pos.setY(y));
            if (ground
                    ? state.blocksMotion() && !state.is(BlockTags.LEAVES)
                    : state.getLightDampening() > 0) {
                return y + 1;
            }
        }
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
