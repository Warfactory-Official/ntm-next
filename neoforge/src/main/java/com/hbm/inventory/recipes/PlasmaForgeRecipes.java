// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class PlasmaForgeRecipes extends SimpleGenericRecipes<PlasmaForgeRecipe> {

    public static final PlasmaForgeRecipes INSTANCE = new PlasmaForgeRecipes();

    @Override
    protected String registryName() {
        return "plasma_forge";
    }

    @Override
    protected RecipeSerializer<PlasmaForgeRecipe> serializer() {
        return PlasmaForgeRecipe.SERIALIZER;
    }
}
