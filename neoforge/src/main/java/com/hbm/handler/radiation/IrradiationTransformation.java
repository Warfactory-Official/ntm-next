/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.handler.radiation;

import com.hbm.util.DataCodecs;
import com.hbm.util.RegistryUtil;
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
            ResourceKey.createRegistryKey(RegistryUtil.id("irradiation_transformation"));
    public static final Codec<IrradiationTransformation> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<IrradiationTransformation>mapCodec(
                                    i ->
                                            i.group(
                                                            RegistryCodecs
                                                                    .<EntityType<?>>homogeneousList(
                                                                            (ResourceKey<
                                                                                            ? extends
                                                                                                    Registry<
                                                                                                            EntityType<
                                                                                                                    ?>>>)
                                                                                    Registries
                                                                                            .ENTITY_TYPE)
                                                                    .fieldOf("targets")
                                                                    .forGetter(
                                                                            IrradiationTransformation
                                                                                    ::targets),
                                                            Codec.doubleRange(0D, Double.MAX_VALUE)
                                                                    .fieldOf("minimum_dose")
                                                                    .forGetter(
                                                                            IrradiationTransformation
                                                                                    ::minimumDose),
                                                            ((Codec<Holder<EntityType<?>>>)
                                                                            RegistryFixedCodec
                                                                                    .<EntityType<?>>
                                                                                            create(
                                                                                                    (ResourceKey<
                                                                                                                    ? extends
                                                                                                                            Registry<
                                                                                                                                    EntityType<
                                                                                                                                            ?>>>)
                                                                                                            Registries
                                                                                                                    .ENTITY_TYPE))
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
