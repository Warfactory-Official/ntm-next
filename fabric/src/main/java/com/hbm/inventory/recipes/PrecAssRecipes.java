// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PrecAssRecipes extends SimpleGenericRecipes<GenericRecipe> {

    public static final PrecAssRecipes INSTANCE = new PrecAssRecipes();

    @Override
    protected String registryName() {
        return "precision_assembler";
    }

    @Override
    protected RecipeSerializer<PrecAssRecipe> serializer() {
        return PrecAssRecipe.SERIALIZER;
    }
}
