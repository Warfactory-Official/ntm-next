// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.lib.Library;
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
            ResourceKey.createRegistryKey(Library.id("rad_resistance"));

    public static final Codec<RadResistanceRule> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            RegistryCodecs.homogeneousList(Registries.BLOCK)
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
