// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge;

import com.hbm.inventory.recipes.loader.RecipeConditions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;

public record FlagCondition(String literal) implements ICondition {

    public static final MapCodec<FlagCondition> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            RecipeConditions.LITERAL_CODEC
                                                    .fieldOf("name")
                                                    .forGetter(FlagCondition::literal))
                                    .apply(instance, FlagCondition::new));

    @Override
    public boolean test(IContext context) {
        return RecipeConditions.test(literal);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
