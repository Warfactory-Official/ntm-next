// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.world.gen.WorldgenHeight;
import com.hbm.world.gen.component.*;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class NtmFeaturesStructure extends Structure {

    public static final MapCodec<NtmFeaturesStructure> CODEC =
            simpleCodec(NtmFeaturesStructure::new);

    private static final int FLATNESS_SAMPLE_RADIUS = 21;
    private static final int FLATNESS_TOLERANCE_BLOCKS = 4;

    public NtmFeaturesStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);
        RandomSource random = context.random();

        int centerHeight =
                context.chunkGenerator()
                        .getFirstOccupiedHeight(
                                x,
                                z,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState());
        int meanHeight =
                getMeanFirstOccupiedHeight(
                        context,
                        x - FLATNESS_SAMPLE_RADIUS,
                        FLATNESS_SAMPLE_RADIUS * 2,
                        z - FLATNESS_SAMPLE_RADIUS,
                        FLATNESS_SAMPLE_RADIUS * 2);
        boolean flat = Math.abs(meanHeight - centerHeight) <= FLATNESS_TOLERANCE_BLOCKS;

        Holder<Biome> biomeHolder =
                context.chunkGenerator()
                        .getBiomeSource()
                        .getNoiseBiome(
                                QuartPos.fromBlock(x),
                                QuartPos.fromBlock(centerHeight),
                                QuartPos.fromBlock(z),
                                context.randomState().sampler());
        Biome biome = biomeHolder.value();
        boolean hotDryNonMesa =
                biome.getBaseTemperature() >= 1.0F
                        && !biome.hasPrecipitation()
                        && !biomeHolder.is(BiomeTags.IS_BADLANDS);

        BlockPos origin = new BlockPos(x, centerHeight, z);

        if (flat && random.nextInt(10) == 0) {
            return Optional.of(
                    new Structure.GenerationStub(
                            origin,
                            builder ->
                                    builder.addPiece(SiloComponentPiece.create(context, origin))));
        }
        if (hotDryNonMesa) {
            NtmComponentPiece piece =
                    random.nextBoolean()
                            ? new NTMHouse1Piece(origin, random)
                            : new NTMHouse2Piece(origin, random);
            return grounded(context, origin, piece);
        }
        NtmComponentPiece piece =
                switch (random.nextInt(6)) {
                    case 0 -> new NTMLab2Piece(origin, random);
                    case 1 -> new NTMLab1Piece(origin, random);
                    case 2 -> new LargeOfficePiece(origin, random);
                    case 3 -> new LargeOfficeCornerPiece(origin, random);
                    default -> new RuralHouse1Piece(origin, random);
                };
        return grounded(context, origin, piece);
    }

    private static Optional<GenerationStub> grounded(
            GenerationContext context, BlockPos origin, NtmComponentPiece piece) {
        int surface = WorldgenHeight.averageGround(context, piece.getBoundingBox());

        piece.move(0, surface - origin.getY(), 0);
        return Optional.of(
                new GenerationStub(
                        new BlockPos(origin.getX(), surface, origin.getZ()),
                        builder -> builder.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.NTM_FEATURES.get();
    }
}
