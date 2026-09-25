// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class CrystallizerRecipes extends SimpleGenericRecipes<CrystallizerRecipe> {

    public static final CrystallizerRecipes INSTANCE = new CrystallizerRecipes();

    @Override
    protected String registryName() {
        return "crystallizer";
    }

    @Override
    protected RecipeSerializer<CrystallizerRecipe> serializer() {
        return CrystallizerRecipe.SERIALIZER;
    }

    public int getAmount(ItemStack stack) {
        CrystallizerRecipe recipe = findByItem(stack, r -> r.inputItem[0].matchesItem(stack));
        return recipe == null ? 0 : recipe.itemAmount();
    }

    public @Nullable CrystallizerRecipe getOutput(ItemStack stack, @Nullable Fluid type) {
        if (stack.isEmpty() || type == null) return null;
        return findByItem(stack, r -> r.acidType() == type && r.inputItem[0].matchesItem(stack));
    }
}
