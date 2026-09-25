// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.NtmRecipeInput;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class OutgasserRecipes extends SimpleGenericRecipes<OutgasserRecipe> {

    public static final OutgasserRecipes INSTANCE = new OutgasserRecipes();

    public static final int DURATION = 10_000;

    @Override
    protected String registryName() {
        return "outgasser";
    }

    @Override
    protected RecipeSerializer<OutgasserRecipe> serializer() {
        return OutgasserRecipe.SERIALIZER;
    }

    public @Nullable OutgasserRecipe getRecipe(ItemStack input, Level level) {
        if (input.isEmpty()) return null;
        NtmRecipeInput in = new NtmRecipeInput(1).set(0, input);
        return findByItem(input, r -> !r.fusionOnly && r.matches(in, level));
    }

    public @Nullable OutgasserRecipe getRecipeForFusion(ItemStack input, Level level) {
        if (input.isEmpty()) return null;
        NtmRecipeInput in = new NtmRecipeInput(1).set(0, input);
        return findByItem(input, r -> r.matches(in, level));
    }
}
