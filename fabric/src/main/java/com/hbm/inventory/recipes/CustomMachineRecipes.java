// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.RecipeIndex;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CustomMachineRecipes
        extends GenericRecipes<CustomMachineRecipe, RecipeIndex<String, CustomMachineRecipe>> {

    public static final CustomMachineRecipes INSTANCE = new CustomMachineRecipes();

    public static List<CustomMachineRecipe> byKey(String recipeKey) {
        return INSTANCE.index().candidates(recipeKey);
    }

    @Override
    protected String registryName() {
        return "custom_machine";
    }

    @Override
    protected RecipeSerializer<CustomMachineRecipe> serializer() {
        return CustomMachineRecipe.SERIALIZER;
    }

    @Override
    protected RecipeIndex<String, CustomMachineRecipe> indexRows(List<CustomMachineRecipe> rows) {
        return RecipeIndex.of(rows, recipe -> recipe.recipeKey);
    }
}
