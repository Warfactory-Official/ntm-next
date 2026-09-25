// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.RecipeIndex;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class MixerRecipes extends GenericRecipes<MixerRecipe, RecipeIndex<Fluid, MixerRecipe>> {

    public static final MixerRecipes INSTANCE = new MixerRecipes();

    @Override
    protected String registryName() {
        return "mixer";
    }

    @Override
    protected RecipeSerializer<MixerRecipe> serializer() {
        return MixerRecipe.SERIALIZER;
    }

    @Override
    protected RecipeIndex<Fluid, MixerRecipe> indexRows(List<MixerRecipe> rows) {
        return RecipeIndex.of(rows, recipe -> recipe.outputFluid[0].type());
    }

    public List<MixerRecipe> getOutput(@Nullable Fluid type) {
        return index().candidates(type);
    }
}
