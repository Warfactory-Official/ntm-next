// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class LemegetonRecipes extends SimpleGenericRecipes<LemegetonRecipe> {

    public static final LemegetonRecipes INSTANCE = new LemegetonRecipes();

    @Override
    protected String registryName() {
        return "lemegeton";
    }

    @Override
    protected RecipeSerializer<LemegetonRecipe> serializer() {
        return LemegetonRecipe.SERIALIZER;
    }

    public ItemStack getRecipe(ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        for (LemegetonRecipe recipe : recipes()) {
            if (recipe.accepts(input)) return recipe.result();
        }
        return ItemStack.EMPTY;
    }
}
