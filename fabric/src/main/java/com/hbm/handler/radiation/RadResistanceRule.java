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

import com.hbm.util.RegistryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record RadResistanceRule(
        HolderSet<Block> targets, Optional<StatePropertiesPredicate> state) {

    public static final ResourceKey<Registry<RadResistanceRule>> REGISTRY =
            ResourceKey.createRegistryKey(RegistryUtil.id("rad_resistance"));

    public static final Codec<RadResistanceRule> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            RegistryCodecs.<Block>homogeneousList(
                                                            (ResourceKey<? extends Registry<Block>>)
                                                                    Registries.BLOCK)
                                                    .fieldOf("targets")
                                                    .forGetter(RadResistanceRule::targets),
                                            StatePropertiesPredicate.CODEC
                                                    .optionalFieldOf("state")
                                                    .forGetter(RadResistanceRule::state))
                                    .apply(instance, RadResistanceRule::new));

    public boolean matches(BlockState candidate) {
        return state.map(predicate -> predicate.matches(candidate)).orElse(true);
    }
}
