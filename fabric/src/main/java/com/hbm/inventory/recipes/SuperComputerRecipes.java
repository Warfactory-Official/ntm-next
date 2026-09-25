// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SuperComputerRecipes extends SimpleGenericRecipes<SuperComputerRecipe> {
    public static final SuperComputerRecipes INSTANCE = new SuperComputerRecipes();

    @Override
    protected String registryName() {
        return "supercomputer";
    }

    @Override
    protected RecipeSerializer<SuperComputerRecipe> serializer() {
        return SuperComputerRecipe.SERIALIZER;
    }
}
