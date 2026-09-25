// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class ReformingRecipes extends SimpleGenericRecipes<ReformingRecipe> {

    public static final ReformingRecipes INSTANCE = new ReformingRecipes();

    public static final long POWER_PER_OP = 20_000L;
    public static final int FILL_PER_OP = 100;

    @Override
    protected String registryName() {
        return "catalytic_reformer";
    }

    @Override
    protected RecipeSerializer<ReformingRecipe> serializer() {
        return ReformingRecipe.SERIALIZER;
    }

    public @Nullable ReformingRecipe getRecipeForInput(@Nullable Fluid type) {
        return byInputFluid().findLast(type);
    }
}
