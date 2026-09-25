// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class CombinationRecipes extends SimpleGenericRecipes<CombinationRecipe> {

    public static final CombinationRecipes INSTANCE = new CombinationRecipes();

    @Override
    protected String registryName() {
        return "combination_oven";
    }

    @Override
    protected RecipeSerializer<CombinationRecipe> serializer() {
        return CombinationRecipe.SERIALIZER;
    }

    public @Nullable CombinationRecipe getOutput(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return findByItem(stack, r -> r.inputItem[0].matchesItem(stack));
    }
}
