// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class MagicRecipes extends SimpleGenericRecipes<MagicRecipe> {

    public static final MagicRecipes INSTANCE = new MagicRecipes();

    @Override
    protected String registryName() {
        return "magic";
    }

    @Override
    protected RecipeSerializer<MagicRecipe> serializer() {
        return MagicRecipe.SERIALIZER;
    }

    public ItemStack getRecipe(Container matrix) {
        List<ItemStack> contents = new ArrayList<>();
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = matrix.getItem(slot);
            if (!stack.isEmpty()) contents.add(stack);
        }
        for (MagicRecipe recipe : recipes()) {
            if (recipe.matchesContents(contents)) return recipe.result().copy();
        }
        return ItemStack.EMPTY;
    }
}
