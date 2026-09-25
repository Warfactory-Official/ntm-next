// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class HydrotreatingRecipes extends SimpleGenericRecipes<HydrotreatingRecipe> {

    public static final HydrotreatingRecipes INSTANCE = new HydrotreatingRecipes();

    public static final long POWER_PER_OP = 20_000L;
    public static final int FILL_PER_OP = 100;

    @Override
    protected String registryName() {
        return "hydrotreater";
    }

    @Override
    protected RecipeSerializer<HydrotreatingRecipe> serializer() {
        return HydrotreatingRecipe.SERIALIZER;
    }

    public @Nullable HydrotreatingRecipe getRecipeForInput(@Nullable Fluid type) {
        return byInputFluid().findLast(type);
    }
}
