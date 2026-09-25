// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class FractionRecipes extends SimpleGenericRecipes<FractionRecipe> {

    public static final FractionRecipes INSTANCE = new FractionRecipes();

    public static final int FILL_PER_OP = 100;

    @Override
    protected String registryName() {
        return "fraction_tower";
    }

    @Override
    protected RecipeSerializer<FractionRecipe> serializer() {
        return FractionRecipe.SERIALIZER;
    }

    public @Nullable FractionRecipe getFractions(@Nullable Fluid oil) {
        return byInputFluid().findLast(oil);
    }
}
