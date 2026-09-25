// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class RefineryRecipes extends SimpleGenericRecipes<RefineryRecipe> {

    public static final RefineryRecipes INSTANCE = new RefineryRecipes();

    public static final int FILL_PER_OP = 100;

    public static final int OIL_FRAC_HEAVY = 50;
    public static final int OIL_FRAC_NAPH = 25;
    public static final int OIL_FRAC_LIGHT = 15;
    public static final int OIL_FRAC_PETRO = 10;
    public static final int CRACK_FRAC_NAPH = 40;
    public static final int CRACK_FRAC_LIGHT = 30;
    public static final int CRACK_FRAC_AROMA = 15;
    public static final int CRACK_FRAC_UNSAT = 15;

    public static final int OILDS_FRAC_HEAVY = 30;
    public static final int OILDS_FRAC_NAPH = 35;
    public static final int OILDS_FRAC_LIGHT = 20;
    public static final int OILDS_FRAC_UNSAT = 15;
    public static final int CRACKDS_FRAC_NAPH = 35;
    public static final int CRACKDS_FRAC_LIGHT = 35;
    public static final int CRACKDS_FRAC_AROMA = 15;
    public static final int CRACKDS_FRAC_UNSAT = 15;

    @Override
    protected String registryName() {
        return "refinery";
    }

    @Override
    protected RecipeSerializer<RefineryRecipe> serializer() {
        return RefineryRecipe.SERIALIZER;
    }

    public @Nullable RefineryRecipe getRefinery(@Nullable Fluid oil) {
        return byInputFluid().findLast(oil);
    }
}
