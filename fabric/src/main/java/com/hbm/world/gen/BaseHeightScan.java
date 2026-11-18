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

import java.util.Arrays;
import java.util.function.Predicate;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
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
        Arrays.fill(out, heightAccessor.getMinY());
        if (noise.height() <= 0) return true;
        for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
            for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
                DensityVolume volume =
                        new DensityVolume(
                                16, noise.height(), 16, chunkX << 4, noise.minY(), chunkZ << 4);
                try (NoiseChunk chunk =
                        new NoiseChunk(
                                randomState,
                                null,
                                settings,
                                generator.globalFluidPicker.get(),
                                Blender.empty(),
                                volume)) {
                    scan(chunk, settings, box, out);
                }
            }
        }
        return true;
    }

    private static void scan(
            NoiseChunk chunk, NoiseGeneratorSettings settings, BoundingBox box, int[] out) {
        DensityVolume volume = chunk.volume();
        Aquifer aquifer = chunk.aquifer();
        int minX = Math.max(box.minX(), volume.minBlockX());
        int maxX = Math.min(box.maxX(), volume.maxBlockX());
        int minZ = Math.max(box.minZ(), volume.minBlockZ());
        int maxZ = Math.min(box.maxZ(), volume.maxBlockZ());
        var finalDensity = chunk.cachingSamplers().get(settings.noiseRouter().finalDensity());
        BlockState previous = settings.defaultBlock();
        boolean opaque = OPAQUE.test(previous);
        try (var density = finalDensity.sampleVolume(volume)) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int y = volume.sizeY() - 1; y >= 0; y--) {
                        int blockY = volume.blockY(y);
                        float value =
                                density.get(
                                        volume.indexUnchecked(
                                                x - volume.minBlockX(), y, z - volume.minBlockZ()));
                        BlockState state = aquifer.computeSubstance(x, blockY, z, value);
                        if (state == null) state = settings.defaultBlock();
                        if (state != previous) {
                            previous = state;
                            opaque = OPAQUE.test(state);
                        }
                        if (opaque) {
                            out[(z - box.minZ()) * box.getXSpan() + x - box.minX()] = blockY + 1;
                            break;
                        }
                    }
                }
            }
        }
    }
}
