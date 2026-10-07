// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.world.gen.nbt.LegacyPlacement;
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
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

public final class LegacyJigsawStructure extends JigsawStructure {
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
        return super.findGenerationPoint(context)
                .map(
                        stub ->
                                new GenerationStub(
                                        stub.position(),
                                        output -> {
                                            List<StructurePiece> pieces =
                                                    stub.getPiecesBuilder().build().pieces();
                                            PoolElementStructurePiece anchor = null;
                                            for (StructurePiece piece : pieces) {
                                                if (!placement(piece).alignToTerrain()) {
                                                    anchor = (PoolElementStructurePiece) piece;
                                                    break;
                                                }
                                            }
                                            if (anchor != null) {
                                                int dy =
                                                        grounded(context, anchor)
                                                                - anchor.getBoundingBox().minY();
                                                for (StructurePiece piece : pieces) {
                                                    if (!placement(piece).alignToTerrain())
                                                        piece.move(0, dy, 0);
                                                }
                                            }
                                            for (StructurePiece piece : pieces) {
                                                if (placement(piece).alignToTerrain()) {
                                                    piece.move(
                                                            0,
                                                            grounded(context, piece)
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

    private int grounded(GenerationContext context, StructurePiece piece) {
        int y = averageGround(context, piece.getBoundingBox()) + placement(piece).heightOffset();
        return context.chunkGenerator() instanceof FlatLevelSource
                ? y
                : Math.clamp(y, minHeight, maxHeight);
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
        return HbmStructureTypes.LEGACY_JIGSAW.get();
    }
}
