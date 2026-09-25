// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.world.structure.HbmStructureTypes;
import com.mojang.datafixers.util.Either;
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

public final class ParentOverlapPoolElement extends SinglePoolElement {
    public static final MapCodec<ParentOverlapPoolElement> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            ParentOverlapPoolElement
                                                    .<ParentOverlapPoolElement>templateCodec(),
                                            ParentOverlapPoolElement
                                                    .<ParentOverlapPoolElement>processorsCodec(),
                                            ParentOverlapPoolElement
                                                    .<ParentOverlapPoolElement>projectionCodec(),
                                            ParentOverlapPoolElement
                                                    .<ParentOverlapPoolElement>
                                                            overrideLiquidSettingsCodec(),
                                            Identifier.CODEC
                                                    .fieldOf("parent_overlap_connector")
                                                    .forGetter(
                                                            element ->
                                                                    element.parentOverlapConnector))
                                    .apply(instance, ParentOverlapPoolElement::new));

    private final Identifier parentOverlapConnector;

    private ParentOverlapPoolElement(
            Either<Identifier, StructureTemplate> template,
            Holder<StructureProcessorList> processors,
            StructureTemplatePool.Projection projection,
            Optional<LiquidSettings> liquidSettings,
            Identifier parentOverlapConnector) {
        super(template, processors, projection, liquidSettings);
        this.parentOverlapConnector = parentOverlapConnector;
    }

    public static Function<StructureTemplatePool.Projection, ParentOverlapPoolElement> single(
            Identifier template,
            Holder<StructureProcessorList> processors,
            Identifier parentOverlapConnector) {
        return projection ->
                new ParentOverlapPoolElement(
                        Either.left(template),
                        processors,
                        projection,
                        Optional.empty(),
                        parentOverlapConnector);
    }

    public boolean allowsParentOverlap(Identifier connector) {
        return parentOverlapConnector.equals(connector);
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return HbmStructureTypes.PARENT_OVERLAP_POOL_ELEMENT.get();
    }
}
