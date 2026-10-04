// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class ColumnBiome {

    private ColumnBiome() {}

    public static boolean valid(
            Structure.GenerationStub stub, Structure.GenerationContext context) {
        BlockPos pos = stub.position();
        ChunkGenerator generator = context.chunkGenerator();
        int surface =
                generator.getBaseHeight(
                        pos.getX(),
                        pos.getZ(),
                        Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(),
                        context.randomState());
        return context.validBiome()
                .test(
                        generator
                                .getBiomeSource()
                                .getNoiseBiome(
                                        QuartPos.fromBlock(pos.getX()),
                                        QuartPos.fromBlock(surface),
                                        QuartPos.fromBlock(pos.getZ()),
                                        context.randomState().sampler()));
    }
}
