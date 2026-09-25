// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class FuelPoolRecipes extends SimpleGenericRecipes<FuelPoolRecipe> {

    public static final FuelPoolRecipes INSTANCE = new FuelPoolRecipes();

    public static @Nullable FuelPoolRecipe getRecipe(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return INSTANCE.findByItem(stack, recipe -> recipe.input().matchesItem(stack));
    }

    @Override
    protected String registryName() {
        return "fuel_pool";
    }

    @Override
    protected RecipeSerializer<FuelPoolRecipe> serializer() {
        return FuelPoolRecipe.SERIALIZER;
    }
}
