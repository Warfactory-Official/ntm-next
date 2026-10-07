// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.world.structure.HbmStructureTypes;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class LegacyPiecePoolElement extends SinglePoolElement implements LegacyPlacement {
    public static final MapCodec<LegacyPiecePoolElement> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            LegacyPiecePoolElement
                                                    .<LegacyPiecePoolElement>templateCodec(),
                                            LegacyPiecePoolElement
                                                    .<LegacyPiecePoolElement>processorsCodec(),
                                            LegacyPiecePoolElement
                                                    .<LegacyPiecePoolElement>projectionCodec(),
                                            LegacyPiecePoolElement
                                                    .<LegacyPiecePoolElement>
                                                            overrideLiquidSettingsCodec(),
                                            Codec.INT
                                                    .optionalFieldOf("height_offset", 0)
                                                    .forGetter(
                                                            LegacyPiecePoolElement::heightOffset),
                                            Codec.BOOL
                                                    .optionalFieldOf("align_to_terrain", false)
                                                    .forGetter(
                                                            LegacyPiecePoolElement::alignToTerrain))
                                    .apply(instance, LegacyPiecePoolElement::new));

    private final int heightOffset;
    private final boolean alignToTerrain;

    private LegacyPiecePoolElement(
            Either<Identifier, StructureTemplate> template,
            Holder<StructureProcessorList> processors,
            StructureTemplatePool.Projection projection,
            Optional<LiquidSettings> liquidSettings,
            int heightOffset,
            boolean alignToTerrain) {
        super(template, processors, projection, liquidSettings);
        this.heightOffset = heightOffset;
        this.alignToTerrain = alignToTerrain;
    }

    public static Function<StructureTemplatePool.Projection, LegacyPiecePoolElement> single(
            Identifier template,
            Holder<StructureProcessorList> processors,
            int heightOffset,
            boolean alignToTerrain) {
        return projection ->
                new LegacyPiecePoolElement(
                        Either.left(template),
                        processors,
                        projection,
                        Optional.empty(),
                        heightOffset,
                        alignToTerrain);
    }

    @Override
    public int heightOffset() {
        return heightOffset;
    }

    @Override
    public boolean alignToTerrain() {
        return alignToTerrain;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return HbmStructureTypes.LEGACY_PIECE_POOL_ELEMENT.get();
    }
}
