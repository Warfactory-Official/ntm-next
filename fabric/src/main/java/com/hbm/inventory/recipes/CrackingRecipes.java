// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class CrackingRecipes extends SimpleGenericRecipes<CrackingRecipe> {

    public static final CrackingRecipes INSTANCE = new CrackingRecipes();

    public static final int FILL_PER_OP = 100;

    @Override
    protected String registryName() {
        return "cracking_tower";
    }

    @Override
    protected RecipeSerializer<CrackingRecipe> serializer() {
        return CrackingRecipe.SERIALIZER;
    }

    public @Nullable CrackingRecipe getCracking(@Nullable Fluid input) {
        return byInputFluid().findLast(input);
    }
}
