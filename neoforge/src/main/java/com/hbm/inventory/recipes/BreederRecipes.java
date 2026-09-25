// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class BreederRecipes extends SimpleGenericRecipes<BreederRecipe> {

    public static final BreederRecipes INSTANCE = new BreederRecipes();

    public static @Nullable BreederRecipe getOutput(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return INSTANCE.findByItem(stack, recipe -> recipe.input().matchesItem(stack));
    }

    @Override
    protected String registryName() {
        return "breeder";
    }

    @Override
    protected RecipeSerializer<BreederRecipe> serializer() {
        return BreederRecipe.SERIALIZER;
    }
}
