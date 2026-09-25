// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class AmmoPressRecipes extends SimpleGenericRecipes<AmmoPressRecipe> {

    public static final AmmoPressRecipes INSTANCE = new AmmoPressRecipes();

    @Override
    protected String registryName() {
        return "ammo_press";
    }

    @Override
    protected RecipeSerializer<AmmoPressRecipe> serializer() {
        return AmmoPressRecipe.SERIALIZER;
    }
}
