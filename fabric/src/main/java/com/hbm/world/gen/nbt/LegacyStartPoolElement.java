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

public final class LegacyStartPoolElement extends SinglePoolElement implements LegacyPlacement {
    public static final MapCodec<LegacyStartPoolElement> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            LegacyStartPoolElement
                                                    .<LegacyStartPoolElement>templateCodec(),
                                            LegacyStartPoolElement
                                                    .<LegacyStartPoolElement>processorsCodec(),
                                            LegacyStartPoolElement
                                                    .<LegacyStartPoolElement>projectionCodec(),
                                            LegacyStartPoolElement
                                                    .<LegacyStartPoolElement>
                                                            overrideLiquidSettingsCodec(),
                                            Codec.intRange(1, 1024)
                                                    .fieldOf("size_limit")
                                                    .forGetter(LegacyStartPoolElement::sizeLimit),
                                            Codec.intRange(1, 128)
                                                    .fieldOf("range_limit")
                                                    .forGetter(LegacyStartPoolElement::rangeLimit),
                                            Codec.INT
                                                    .optionalFieldOf("height_offset", 0)
                                                    .forGetter(
                                                            LegacyStartPoolElement::heightOffset),
                                            Codec.BOOL
                                                    .optionalFieldOf("align_to_terrain", false)
                                                    .forGetter(
                                                            LegacyStartPoolElement::alignToTerrain))
                                    .apply(instance, LegacyStartPoolElement::new));

    private final int sizeLimit;
    private final int rangeLimit;
    private final int heightOffset;
    private final boolean alignToTerrain;

    private LegacyStartPoolElement(
            Either<Identifier, StructureTemplate> template,
            Holder<StructureProcessorList> processors,
            StructureTemplatePool.Projection projection,
            Optional<LiquidSettings> liquidSettings,
            int sizeLimit,
            int rangeLimit,
            int heightOffset,
            boolean alignToTerrain) {
        super(template, processors, projection, liquidSettings);
        this.sizeLimit = sizeLimit;
        this.rangeLimit = rangeLimit;
        this.heightOffset = heightOffset;
        this.alignToTerrain = alignToTerrain;
    }

    public static Function<StructureTemplatePool.Projection, LegacyStartPoolElement> single(
            Identifier template,
            Holder<StructureProcessorList> processors,
            int sizeLimit,
            int rangeLimit) {
        return projection ->
                new LegacyStartPoolElement(
                        Either.left(template),
                        processors,
                        projection,
                        Optional.empty(),
                        sizeLimit,
                        rangeLimit,
                        0,
                        false);
    }

    public int sizeLimit() {
        return sizeLimit;
    }

    public int rangeLimit() {
        return rangeLimit;
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
        return HbmStructureTypes.LEGACY_START_POOL_ELEMENT.get();
    }
}
