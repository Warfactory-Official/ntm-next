// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class ElectrolyserFluidRecipes extends SimpleGenericRecipes<ElectrolyserFluidRecipe> {

    public static final ElectrolyserFluidRecipes INSTANCE = new ElectrolyserFluidRecipes();

    @Override
    protected String registryName() {
        return "electrolyser_fluid";
    }

    @Override
    protected RecipeSerializer<ElectrolyserFluidRecipe> serializer() {
        return ElectrolyserFluidRecipe.SERIALIZER;
    }

    public @Nullable ElectrolyserFluidRecipe getRecipe(@Nullable Fluid type) {
        return byInputFluid().findLast(type);
    }
}
