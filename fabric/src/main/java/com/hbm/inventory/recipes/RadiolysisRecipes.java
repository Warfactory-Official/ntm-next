// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class RadiolysisRecipes extends SimpleGenericRecipes<RadiolysisRecipe> {

    public static final RadiolysisRecipes INSTANCE = new RadiolysisRecipes();

    public static final int FILL_PER_OP = CrackingRecipes.FILL_PER_OP;

    @Override
    protected String registryName() {
        return "radiolysis";
    }

    @Override
    protected RecipeSerializer<RadiolysisRecipe> serializer() {
        return RadiolysisRecipe.SERIALIZER;
    }

    public @Nullable RadiolysisRecipe getRadiolysis(@Nullable Fluid input) {
        return byInputFluid().findLast(input);
    }
}
