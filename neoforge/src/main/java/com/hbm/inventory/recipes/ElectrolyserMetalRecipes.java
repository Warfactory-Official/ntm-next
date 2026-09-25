// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class ElectrolyserMetalRecipes extends SimpleGenericRecipes<ElectrolyserMetalRecipe> {

    public static final ElectrolyserMetalRecipes INSTANCE = new ElectrolyserMetalRecipes();

    @Override
    protected String registryName() {
        return "electrolyser_metal";
    }

    @Override
    protected RecipeSerializer<ElectrolyserMetalRecipe> serializer() {
        return ElectrolyserMetalRecipe.SERIALIZER;
    }

    public @Nullable ElectrolyserMetalRecipe getRecipe(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return findByItem(stack, r -> r.inputItem[0].matchesItem(stack));
    }
}
