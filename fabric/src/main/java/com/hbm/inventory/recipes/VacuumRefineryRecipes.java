// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class VacuumRefineryRecipes extends SimpleGenericRecipes<VacuumRefineryRecipe> {

    public static final VacuumRefineryRecipes INSTANCE = new VacuumRefineryRecipes();

    public static final int FILL_PER_OP = 100;

    public static final int VAC_FRAC_HEAVY = 40;
    public static final int VAC_FRAC_REFORM = 25;
    public static final int VAC_FRAC_LIGHT = 20;
    public static final int VAC_FRAC_SOUR = 15;

    @Override
    protected String registryName() {
        return "vacuum_refinery";
    }

    @Override
    protected RecipeSerializer<VacuumRefineryRecipe> serializer() {
        return VacuumRefineryRecipe.SERIALIZER;
    }

    public @Nullable VacuumRefineryRecipe getVacuum(@Nullable Fluid oil) {
        return byInputFluid().findLast(oil);
    }
}
