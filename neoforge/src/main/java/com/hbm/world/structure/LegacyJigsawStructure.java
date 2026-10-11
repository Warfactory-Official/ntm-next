// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.world.gen.WorldgenHeight;
import com.hbm.world.gen.nbt.LegacyPlacement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

public final class LegacyJigsawStructure extends JigsawStructure implements ColumnBiome.SelfTested {
    public static final MapCodec<LegacyJigsawStructure> CODEC =
            RecordCodecBuilder.<LegacyJigsawStructure>mapCodec(
                            instance ->
                                    instance.group(
                                                    settingsCodec(instance),
                                                    StructureTemplatePool.CODEC
                                                            .fieldOf("start_pool")
                                                            .forGetter(
                                                                    LegacyJigsawStructure
                                                                            ::getStartPool),
                                                    Codec.intRange(1, 20)
                                                            .fieldOf("size")
                                                            .forGetter(
                                                                    structure ->
                                                                            structure.maxDepth),
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
                                            .apply(instance, LegacyJigsawStructure::new))
                    .validate(
                            structure ->
                                    structure.minHeight <= structure.maxHeight
                                            ? DataResult.success(structure)
                                            : DataResult.error(
                                                    () -> "Inverted legacy jigsaw height band"));

    private final int maxDepth;
    private final int minHeight;
    private final int maxHeight;

    public LegacyJigsawStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> pool,
            int maxDepth,
            int minHeight,
            int maxHeight) {
        super(
                settings,
                pool,
                Optional.empty(),
                maxDepth,
                ConstantHeight.of(VerticalAnchor.absolute((minHeight + maxHeight) / 2)),
                false,
                Optional.empty(),
                new MaxDistance(128),
                List.of(),
                DEFAULT_DIMENSION_PADDING,
                DEFAULT_LIQUID_SETTINGS);
        this.maxDepth = maxDepth;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Optional<GenerationStub> point =
                super.findGenerationPoint(context).filter(stub -> ColumnBiome.valid(stub, context));
        return point.map(
                stub ->
                        new GenerationStub(
                                stub.position(),
                                output -> {
                                    List<StructurePiece> pieces =
                                            stub.getPiecesBuilder().build().pieces();
                                    int[] averages =
                                            context.chunkGenerator()
                                                                    instanceof
                                                                    StructureGrounding grounding
                                                            && grounding.modifiesGround(
                                                                    this, context)
                                                    ? grounding.groundAverages(context, pieces)
                                                    : null;
                                    PoolElementStructurePiece anchor = null;
                                    int anchorIndex = 0;
                                    for (int i = 0; i < pieces.size(); i++) {
                                        StructurePiece piece = pieces.get(i);
                                        if (!placement(piece).alignToTerrain()) {
                                            anchor = (PoolElementStructurePiece) piece;
                                            anchorIndex = i;
                                            break;
                                        }
                                    }
                                    if (anchor != null) {
                                        int dy =
                                                grounded(context, anchor, averages, anchorIndex)
                                                        - anchor.getBoundingBox().minY();
                                        for (StructurePiece piece : pieces) {
                                            if (!placement(piece).alignToTerrain())
                                                piece.move(0, dy, 0);
                                        }
                                    }
                                    for (int i = 0; i < pieces.size(); i++) {
                                        StructurePiece piece = pieces.get(i);
                                        if (placement(piece).alignToTerrain()) {
                                            piece.move(
                                                    0,
                                                    grounded(context, piece, averages, i)
                                                            - piece.getBoundingBox().minY(),
                                                    0);
                                        }
                                    }
                                    pieces.forEach(output::addPiece);
                                }));
    }

    private static LegacyPlacement placement(StructurePiece piece) {
        if (((PoolElementStructurePiece) piece).getElement() instanceof LegacyPlacement placement)
            return placement;
        throw new IllegalStateException(
                "legacy jigsaw piece "
                        + ((PoolElementStructurePiece) piece).getElement()
                        + " declares no legacy placement");
    }

    private int grounded(
            GenerationContext context, StructurePiece piece, int[] averages, int index) {
        int y =
                (averages == null
                                ? WorldgenHeight.averageBaseHeight(context, piece.getBoundingBox())
                                : averages[index])
                        + placement(piece).heightOffset();
        return context.chunkGenerator() instanceof FlatLevelSource ? y : clampHeight(y);
    }

    public int clampHeight(int height) {
        return Math.clamp(height, minHeight, maxHeight);
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.LEGACY_JIGSAW.get();
    }
}
