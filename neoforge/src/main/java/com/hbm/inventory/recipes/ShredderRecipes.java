// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class ShredderRecipes extends SimpleGenericRecipes<ShredderRecipe> {

    public static final ShredderRecipes INSTANCE = new ShredderRecipes();

    public static ItemStack getShredderResult(ItemStack stack) {
        ShredderRecipe recipe = getRecipe(stack);
        return recipe == null ? new ItemStack(ModItems.SCRAP) : recipe.output().copy();
    }

    public static @Nullable ShredderRecipe getRecipe(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return INSTANCE.findByItem(stack, recipe -> recipe.input().matchesItem(stack));
    }

    @Override
    protected String registryName() {
        return "shredder";
    }

    @Override
    protected RecipeSerializer<ShredderRecipe> serializer() {
        return ShredderRecipe.SERIALIZER;
    }
}
