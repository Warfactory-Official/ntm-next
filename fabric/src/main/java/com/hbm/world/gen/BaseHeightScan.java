// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen;

import java.util.Arrays;
import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class BaseHeightScan {
    private static final Predicate<BlockState> OPAQUE = Heightmap.Types.OCEAN_FLOOR_WG.isOpaque();

    private BaseHeightScan() {}

    static boolean fill(
            NoiseBasedChunkGenerator generator,
            LevelHeightAccessor heightAccessor,
            RandomState randomState,
            BoundingBox box,
            int[] out) {
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        NoiseSettings noise = settings.noiseSettings().clampToHeightAccessor(heightAccessor);
        if (16 % noise.getCellWidth() != 0) return false;
        Arrays.fill(out, heightAccessor.getMinY());
        int cellCountY = Mth.floorDiv(noise.height(), noise.getCellHeight());
        if (cellCountY <= 0) return true;
        Aquifer.FluidPicker fluids = generator.globalFluidPicker.get();
        int cellCount = 16 / noise.getCellWidth();
        for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
            for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
                NoiseChunk chunk =
                        new NoiseChunk(
                                cellCount,
                                randomState,
                                chunkX << 4,
                                chunkZ << 4,
                                noise,
                                DensityFunctions.BeardifierMarker.INSTANCE,
                                settings,
                                fluids,
                                Blender.empty());
                scan(
                        chunk,
                        chunkX << 4,
                        chunkZ << 4,
                        cellCount,
                        noise,
                        settings.defaultBlock(),
                        cellCountY,
                        box,
                        out);
            }
        }
        return true;
    }

    private static void scan(
            NoiseChunk chunk,
            int minBlockX,
            int minBlockZ,
            int cellCount,
            NoiseSettings noise,
            BlockState defaultBlock,
            int cellCountY,
            BoundingBox box,
            int[] out) {
        int width = noise.getCellWidth();
        int height = noise.getCellHeight();
        int cellMinY = Mth.floorDiv(noise.minY(), height);
        int minX = Math.max(box.minX(), minBlockX);
        int maxX = Math.min(box.maxX(), minBlockX + 15);
        int minZ = Math.max(box.minZ(), minBlockZ);
        int maxZ = Math.min(box.maxZ(), minBlockZ + 15);
        int xSpan = box.getXSpan();
        boolean[] resolved = new boolean[width * width];
        chunk.initializeForFirstCellX();
        for (int cellX = 0; cellX <= (maxX - minBlockX) / width; cellX++) {
            chunk.advanceCellX(cellX);
            int cellStartX = minBlockX + cellX * width;
            int x0 = Math.max(minX, cellStartX);
            int x1 = Math.min(maxX, cellStartX + width - 1);
            for (int cellZ = 0; cellZ < cellCount && x0 <= x1; cellZ++) {
                int cellStartZ = minBlockZ + cellZ * width;
                int z0 = Math.max(minZ, cellStartZ);
                int z1 = Math.min(maxZ, cellStartZ + width - 1);
                if (z0 > z1) continue;
                Arrays.fill(resolved, false);
                int pending = (x1 - x0 + 1) * (z1 - z0 + 1);
                for (int cellY = cellCountY - 1; cellY >= 0 && pending > 0; cellY--) {
                    chunk.selectCellYZ(cellY, cellZ);
                    for (int yInCell = height - 1; yInCell >= 0 && pending > 0; yInCell--) {
                        int posY = (cellMinY + cellY) * height + yInCell;
                        chunk.updateForY(posY, (double) yInCell / height);
                        for (int x = x0; x <= x1; x++) {
                            for (int z = z0; z <= z1; z++) {
                                int column = (x - cellStartX) * width + z - cellStartZ;
                                if (resolved[column]) continue;
                                chunk.updateForX(x, (double) (x - cellStartX) / width);
                                chunk.updateForZ(z, (double) (z - cellStartZ) / width);
                                BlockState state = chunk.getInterpolatedState();
                                if (OPAQUE.test(state == null ? defaultBlock : state)) {
                                    out[(z - box.minZ()) * xSpan + x - box.minX()] = posY + 1;
                                    resolved[column] = true;
                                    pending--;
                                }
                            }
                        }
                    }
                }
            }
            chunk.swapSlices();
        }
        chunk.stopInterpolation();
    }
}
