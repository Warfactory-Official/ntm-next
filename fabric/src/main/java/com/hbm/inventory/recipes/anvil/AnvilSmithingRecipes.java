// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class AnvilSmithingRecipes extends SimpleGenericRecipes<AnvilSmithingRecipe> {

    public static final AnvilSmithingRecipes INSTANCE = new AnvilSmithingRecipes();

    @Override
    protected String registryName() {
        return "anvil_smithing";
    }

    @Override
    protected RecipeSerializer<AnvilSmithingRecipe> serializer() {
        return AnvilSmithingRecipe.SERIALIZER;
    }
}
