// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public final class SpaceAssemblerRecipes extends SimpleGenericRecipes<SpaceAssemblerRecipe> {

    public static final SpaceAssemblerRecipes INSTANCE = new SpaceAssemblerRecipes();

    private SpaceAssemblerRecipes() {}

    @Override
    protected String registryName() {
        return "space_assembler";
    }

    @Override
    protected RecipeSerializer<SpaceAssemblerRecipe> serializer() {
        return SpaceAssemblerRecipe.SERIALIZER;
    }

    public @Nullable SpaceAssemblerRecipe getRecipeFor(ItemStack stack) {
        for (SpaceAssemblerRecipe recipe : recipes()) {
            if (recipe.inputItem.length == 1 && recipe.inputItem[0].test(stack)) return recipe;
        }
        return null;
    }
}
