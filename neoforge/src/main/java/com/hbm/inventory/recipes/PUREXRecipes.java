// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PUREXRecipes extends SimpleGenericRecipes<PUREXRecipe> {

    public static final PUREXRecipes INSTANCE = new PUREXRecipes();

    @Override
    protected String registryName() {
        return "purex";
    }

    @Override
    protected RecipeSerializer<PUREXRecipe> serializer() {
        return PUREXRecipe.SERIALIZER;
    }
}
