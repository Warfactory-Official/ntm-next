// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ChemicalPlantRecipes extends SimpleGenericRecipes<ChemicalPlantRecipe> {

    public static final ChemicalPlantRecipes INSTANCE = new ChemicalPlantRecipes();

    @Override
    protected String registryName() {
        return "chemical_plant";
    }

    @Override
    protected RecipeSerializer<ChemicalPlantRecipe> serializer() {
        return ChemicalPlantRecipe.SERIALIZER;
    }
}
