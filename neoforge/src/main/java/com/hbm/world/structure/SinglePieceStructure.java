// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

public final class SinglePieceStructure extends JigsawStructure {
    public static final MapCodec<SinglePieceStructure> CODEC =
            RecordCodecBuilder.<SinglePieceStructure>mapCodec(
                            instance ->
                                    instance.group(
                                                    settingsCodec(instance),
                                                    StructureTemplatePool.CODEC
                                                            .fieldOf("start_pool")
                                                            .forGetter(
                                                                    SinglePieceStructure
                                                                            ::getStartPool),
                                                    Codec.INT
                                                            .fieldOf("height_offset")
                                                            .forGetter(
                                                                    structure ->
                                                                            structure.heightOffset),
                                                    Codec.INT
                                                            .optionalFieldOf("min_height", 1)
                                                            .forGetter(
                                                                    structure ->
                                                                            structure.minHeight),
                                                    Codec.INT
                                                            .optionalFieldOf("max_height", 128)
                                                            .forGetter(
                                                                    structure ->
                                                                            structure.maxHeight))
                                            .apply(instance, SinglePieceStructure::new))
                    .validate(
                            structure ->
                                    structure.minHeight <= structure.maxHeight
                                            ? DataResult.success(structure)
                                            : DataResult.error(
                                                    () -> "Inverted single-piece height band"));

    private final int heightOffset;
    private final int minHeight;
    private final int maxHeight;

    public SinglePieceStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> pool,
            int heightOffset,
            int minHeight,
            int maxHeight) {

        super(
                settings,
                pool,
                Optional.empty(),
                1,
                ConstantHeight.of(VerticalAnchor.absolute(heightOffset + 1)),
                false,
                Optional.of(Heightmap.Types.OCEAN_FLOOR_WG),
                new MaxDistance(128),
                List.of(),
                DEFAULT_DIMENSION_PADDING,
                DEFAULT_LIQUID_SETTINGS);
        this.heightOffset = heightOffset;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {

        return super.findGenerationPoint(context)
                .map(
                        stub ->
                                new GenerationStub(
                                        stub.position(),
                                        output -> {
                                            var builder = stub.getPiecesBuilder();
                                            var pieces = builder.build().pieces();
                                            if (pieces.size() != 1)
                                                throw new IllegalStateException(
                                                        "Single-piece pool "
                                                                + getStartPool()
                                                                + " produced "
                                                                + pieces.size()
                                                                + " pieces");
                                            PoolElementStructurePiece piece =
                                                    (PoolElementStructurePiece) pieces.getFirst();
                                            int y =
                                                    averageGround(context, piece.getBoundingBox())
                                                            + heightOffset;

                                            if (!(context.chunkGenerator()
                                                    instanceof FlatLevelSource))
                                                y = Math.clamp(y, minHeight, maxHeight);
                                            piece.move(0, y - piece.getBoundingBox().minY(), 0);
                                            output.addPiece(piece);
                                        }));
    }

    private static int averageGround(GenerationContext context, BoundingBox footprint) {
        long total = 0;
        for (int z = footprint.minZ(); z <= footprint.maxZ(); z++) {
            for (int x = footprint.minX(); x <= footprint.maxX(); x++) {
                total +=
                        context.chunkGenerator()
                                .getBaseHeight(
                                        x,
                                        z,
                                        Heightmap.Types.OCEAN_FLOOR_WG,
                                        context.heightAccessor(),
                                        context.randomState());
            }
        }
        return (int) (total / ((long) footprint.getXSpan() * footprint.getZSpan()));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.SINGLE_PIECE.get();
    }
}
