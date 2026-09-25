// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class CokerRecipes extends SimpleGenericRecipes<CokerRecipe> {

    public static final CokerRecipes INSTANCE = new CokerRecipes();

    @Override
    protected String registryName() {
        return "coker";
    }

    @Override
    protected RecipeSerializer<CokerRecipe> serializer() {
        return CokerRecipe.SERIALIZER;
    }

    public @Nullable CokerRecipe getOutput(@Nullable Fluid type) {
        return byInputFluid().findLast(type);
    }
}
