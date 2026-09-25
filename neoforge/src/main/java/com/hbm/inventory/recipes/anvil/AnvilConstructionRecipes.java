// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class AnvilConstructionRecipes extends SimpleGenericRecipes<AnvilConstructionRecipe> {

    public static final AnvilConstructionRecipes INSTANCE = new AnvilConstructionRecipes();

    @Override
    protected String registryName() {
        return "anvil_construction";
    }

    @Override
    protected RecipeSerializer<AnvilConstructionRecipe> serializer() {
        return AnvilConstructionRecipe.SERIALIZER;
    }
}
