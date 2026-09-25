// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class FluidBreederRecipes extends SimpleGenericRecipes<FluidBreederRecipe> {

    public static final FluidBreederRecipes INSTANCE = new FluidBreederRecipes();

    @Override
    protected String registryName() {
        return "fluid_breeder";
    }

    @Override
    protected RecipeSerializer<FluidBreederRecipe> serializer() {
        return FluidBreederRecipe.SERIALIZER;
    }

    public @Nullable FluidBreederRecipe getOutput(@Nullable Fluid type) {
        return byInputFluid().findLast(type);
    }
}
