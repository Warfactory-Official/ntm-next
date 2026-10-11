// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.world.gen.WorldgenHeight;
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

public final class SinglePieceStructure extends JigsawStructure implements ColumnBiome.SelfTested {
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
                                                                            structure.maxHeight),
                                                    Codec.BOOL
                                                            .optionalFieldOf("aquatic", false)
                                                            .forGetter(
                                                                    structure -> structure.aquatic))
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
    private final boolean aquatic;

    public SinglePieceStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> pool,
            int heightOffset,
            int minHeight,
            int maxHeight,
            boolean aquatic) {
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
        this.aquatic = aquatic;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return super.findGenerationPoint(context)
                .filter(stub -> ColumnBiome.valid(stub, context))
                .flatMap(
                        stub -> {
                            var pieces = stub.getPiecesBuilder().build().pieces();
                            if (pieces.size() != 1)
                                throw new IllegalStateException(
                                        "Single-piece pool "
                                                + getStartPool()
                                                + " produced "
                                                + pieces.size()
                                                + " pieces");
                            PoolElementStructurePiece piece =
                                    (PoolElementStructurePiece) pieces.getFirst();
                            BoundingBox footprint = piece.getBoundingBox();
                            if (!aquatic
                                    && getLowestY(
                                                    context,
                                                    footprint.minX(),
                                                    footprint.minZ(),
                                                    footprint.getXSpan() - 1,
                                                    footprint.getZSpan() - 1)
                                            < context.chunkGenerator().getSeaLevel())
                                return Optional.empty();
                            return Optional.of(
                                    new GenerationStub(
                                            stub.position(),
                                            output -> {
                                                int y =
                                                        WorldgenHeight.averageBaseHeight(
                                                                        context,
                                                                        piece.getBoundingBox())
                                                                + heightOffset;
                                                if (!(context.chunkGenerator()
                                                        instanceof FlatLevelSource))
                                                    y = Math.clamp(y, minHeight, maxHeight);
                                                piece.move(0, y - piece.getBoundingBox().minY(), 0);
                                                output.addPiece(piece);
                                            }));
                        });
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.SINGLE_PIECE.get();
    }
}
