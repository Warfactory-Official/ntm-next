// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.blocks.network.GeneratedConductors;
import com.hbm.world.structure.HbmStructureTypes;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

public final class ConductorPoolElement extends SinglePoolElement {
    public static final MapCodec<ConductorPoolElement> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            ConductorPoolElement
                                                    .<ConductorPoolElement>templateCodec(),
                                            ConductorPoolElement
                                                    .<ConductorPoolElement>processorsCodec(),
                                            ConductorPoolElement
                                                    .<ConductorPoolElement>projectionCodec(),
                                            ConductorPoolElement
                                                    .<ConductorPoolElement>
                                                            overrideLiquidSettingsCodec(),
                                            Codec.LONG_STREAM
                                                    .xmap(LongStream::toArray, Arrays::stream)
                                                    .fieldOf("conductors")
                                                    .forGetter(element -> element.conductors))
                                    .apply(instance, ConductorPoolElement::new));

    private final long[] conductors;

    private ConductorPoolElement(
            Either<Identifier, StructureTemplate> template,
            Holder<StructureProcessorList> processors,
            StructureTemplatePool.Projection projection,
            Optional<LiquidSettings> liquidSettings,
            long[] conductors) {
        super(template, processors, projection, liquidSettings);
        if (projection != StructureTemplatePool.Projection.RIGID || conductors.length == 0) {
            throw new IllegalArgumentException(
                    "Conductor template needs a rigid projection and nonempty positions: "
                            + template);
        }
        this.conductors = conductors;
    }

    public static Function<StructureTemplatePool.Projection, ConductorPoolElement> single(
            Identifier template, Holder<StructureProcessorList> processors, long[] conductors) {
        return projection ->
                new ConductorPoolElement(
                        Either.left(template),
                        processors,
                        projection,
                        Optional.empty(),
                        conductors);
    }

    @Override
    public boolean place(
            StructureTemplateManager templates,
            WorldGenLevel level,
            StructureManager structures,
            ChunkGenerator generator,
            BlockPos origin,
            BlockPos reference,
            Rotation rotation,
            BoundingBox clip,
            RandomSource random,
            LiquidSettings liquids,
            boolean keepJigsaws) {
        boolean placed =
                super.place(
                        templates,
                        level,
                        structures,
                        generator,
                        origin,
                        reference,
                        rotation,
                        clip,
                        random,
                        liquids,
                        keepJigsaws);
        if (placed) GeneratedConductors.record(level, origin, rotation, clip, conductors);
        return placed;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return HbmStructureTypes.CONDUCTOR_POOL_ELEMENT.get();
    }
}
