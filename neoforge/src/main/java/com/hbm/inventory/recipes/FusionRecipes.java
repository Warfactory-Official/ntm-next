// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class FusionRecipes extends GenericRecipes<FusionRecipe, FusionRecipes.Index> {

    public static final FusionRecipes INSTANCE = new FusionRecipes();

    @Override
    protected String registryName() {
        return "fusion";
    }

    @Override
    protected RecipeSerializer<FusionRecipe> serializer() {
        return FusionRecipe.SERIALIZER;
    }

    @Override
    protected Index indexRows(List<FusionRecipe> rows) {
        long highest = 0;
        for (FusionRecipe recipe : rows) {
            if (recipe.ignitionTemp > highest) highest = recipe.ignitionTemp;
        }
        return new Index(highest);
    }

    public long maxInput() {
        return index().maxInput();
    }

    record Index(long maxInput) {}
}
