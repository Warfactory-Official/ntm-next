// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.world.gen.component.AncientTombPiece;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class AncientTombStructure extends Structure {

    public static final MapCodec<AncientTombStructure> CODEC =
            simpleCodec(AncientTombStructure::new);

    private static final double SPIKE_RADIUS = 20.0;

    public AncientTombStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);
        int height =
                context.chunkGenerator()
                        .getBaseHeight(
                                x,
                                z,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState());
        int yOff = Math.max(height, 35) - 5;

        BlockPos origin = new BlockPos(x, yOff, z);
        return Optional.of(
                new Structure.GenerationStub(
                        origin, builder -> addPiece(context, builder, x, z, yOff)));
    }

    private static void addPiece(
            Structure.GenerationContext context,
            StructurePiecesBuilder builder,
            int x,
            int z,
            int yOff) {
        RandomSource random = context.random();
        int spikeCount = 36 + random.nextInt(15);
        int[] spikeX = new int[spikeCount];
        int[] spikeZ = new int[spikeCount];
        int[] spikeY = new int[spikeCount];
        double rot = 2.0 * Math.PI / spikeCount;
        for (int i = 0; i < spikeCount; i++) {
            double angle = (i + 1) * rot;
            double variance = 1.0 + random.nextDouble() * 0.4;
            int ix = (int) (x + SPIKE_RADIUS * Math.cos(angle) * variance);
            int iz = (int) (z - SPIKE_RADIUS * Math.sin(angle) * variance);
            int iy =
                    context.chunkGenerator()
                                    .getBaseHeight(
                                            ix,
                                            iz,
                                            Heightmap.Types.WORLD_SURFACE_WG,
                                            context.heightAccessor(),
                                            context.randomState())
                            - 3;
            spikeX[i] = ix;
            spikeZ[i] = iz;
            spikeY[i] = iy;
        }
        builder.addPiece(new AncientTombPiece(x, z, yOff, spikeX, spikeZ, spikeY));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.ANCIENT_TOMB.get();
    }
}
