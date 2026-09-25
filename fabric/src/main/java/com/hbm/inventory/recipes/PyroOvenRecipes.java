// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PyroOvenRecipes extends SimpleGenericRecipes<PyroOvenRecipe> {

    public static final PyroOvenRecipes INSTANCE = new PyroOvenRecipes();

    @Override
    protected String registryName() {
        return "pyro_oven";
    }

    @Override
    protected RecipeSerializer<PyroOvenRecipe> serializer() {
        return PyroOvenRecipe.SERIALIZER;
    }
}
