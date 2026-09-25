// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import com.hbm.items.machine.ItemStamp.StampType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class PressRecipes extends SimpleGenericRecipes<PressRecipe> {

    public static final PressRecipes INSTANCE = new PressRecipes();

    @Override
    protected String registryName() {
        return "press";
    }

    @Override
    protected RecipeSerializer<PressRecipe> serializer() {
        return PressRecipe.SERIALIZER;
    }

    public ItemStack getOutput(ItemStack input, @Nullable StampType stampType) {
        PressRecipe recipe = getRecipe(input, stampType);
        return recipe == null ? ItemStack.EMPTY : recipe.output().copy();
    }

    public @Nullable PressRecipe getRecipe(ItemStack input, @Nullable StampType stampType) {
        if (input.isEmpty() || stampType == null) return null;
        return findByItem(
                input, r -> r.stampType == stampType && r.inputItem[0].matchesItem(input));
    }
}
