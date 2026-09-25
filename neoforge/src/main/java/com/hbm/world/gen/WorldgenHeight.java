// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
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
