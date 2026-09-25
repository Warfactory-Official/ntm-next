// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric;

import com.hbm.inventory.recipes.loader.RecipeConditions;
import com.hbm.lib.Library;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.RegistryOps;

public record FlagResourceCondition(String literal) implements ResourceCondition {

    public static final ResourceConditionType<FlagResourceCondition> TYPE =
            ResourceConditionType.create(
                    Library.id("flag"),
                    RecordCodecBuilder.mapCodec(
                            instance ->
                                    instance.group(
                                                    RecipeConditions.LITERAL_CODEC
                                                            .fieldOf("name")
                                                            .forGetter(
                                                                    FlagResourceCondition::literal))
                                            .apply(instance, FlagResourceCondition::new)));

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
        return RecipeConditions.test(literal);
    }
}
