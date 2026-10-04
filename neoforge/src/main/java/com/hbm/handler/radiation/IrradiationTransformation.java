// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.lib.Library;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

public record IrradiationTransformation(
        HolderSet<EntityType<?>> targets,
        double minimumDose,
        Holder<EntityType<?>> result,
        int oneIn,
        float failureDamage) {
    public static final ResourceKey<Registry<IrradiationTransformation>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("irradiation_transformation"));
    public static final Codec<IrradiationTransformation> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<IrradiationTransformation>mapCodec(
                                    i ->
                                            i.group(
                                                            RegistryCodecs.homogeneousList(
                                                                            Registries.ENTITY_TYPE)
                                                                    .fieldOf("targets")
                                                                    .forGetter(
                                                                            IrradiationTransformation
                                                                                    ::targets),
                                                            Codec.doubleRange(0D, Double.MAX_VALUE)
                                                                    .fieldOf("minimum_dose")
                                                                    .forGetter(
                                                                            IrradiationTransformation
                                                                                    ::minimumDose),
                                                            RegistryFixedCodec.create(
                                                                            Registries.ENTITY_TYPE)
                                                                    .fieldOf("result")
                                                                    .forGetter(
                                                                            IrradiationTransformation
                                                                                    ::result),
                                                            DataCodecs.intRange(
                                                                            1, Integer.MAX_VALUE)
                                                                    .optionalFieldOf("one_in", 1)
                                                                    .forGetter(
                                                                            IrradiationTransformation
                                                                                    ::oneIn),
                                                            Codec.floatRange(0F, Float.MAX_VALUE)
                                                                    .optionalFieldOf(
                                                                            "failure_damage", 0F)
                                                                    .forGetter(
                                                                            IrradiationTransformation
                                                                                    ::failureDamage))
                                                    .apply(i, IrradiationTransformation::new)))
                    .codec();
}
