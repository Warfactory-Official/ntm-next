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

    public interface SelfTested {}

    public static boolean valid(
            Structure.GenerationStub stub, Structure.GenerationContext context) {
        BlockPos pos = stub.position();
        return valid(context, pos.getX(), pos.getZ());
    }

    public static boolean valid(Structure.GenerationContext context, int x, int z) {
        return valid(
                context,
                x,
                context.chunkGenerator()
                        .getBaseHeight(
                                x,
                                z,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState()),
                z);
    }

    public static boolean valid(Structure.GenerationContext context, int x, int surface, int z) {
        ChunkGenerator generator = context.chunkGenerator();
        return context.validBiome()
                .test(
                        generator
                                .getBiomeSource()
                                .getNoiseBiome(
                                        QuartPos.fromBlock(x),
                                        QuartPos.fromBlock(surface),
                                        QuartPos.fromBlock(z),
                                        context.randomState().sampler()));
    }
}
