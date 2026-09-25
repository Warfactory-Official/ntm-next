// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class RockMillRecipes extends SimpleGenericRecipes<RockMillRecipe> {
    public static final RockMillRecipes INSTANCE = new RockMillRecipes();

    @Override
    protected String registryName() {
        return "rock_mill";
    }

    @Override
    protected RecipeSerializer<RockMillRecipe> serializer() {
        return RockMillRecipe.SERIALIZER;
    }
}
