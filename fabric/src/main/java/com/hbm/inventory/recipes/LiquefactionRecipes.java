// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public class LiquefactionRecipes extends SimpleGenericRecipes<LiquefactionRecipe> {

    public static final LiquefactionRecipes INSTANCE = new LiquefactionRecipes();

    @Override
    protected String registryName() {
        return "liquefaction";
    }

    @Override
    protected RecipeSerializer<LiquefactionRecipe> serializer() {
        return LiquefactionRecipe.SERIALIZER;
    }

    public @Nullable FluidStackNTM getOutput(ItemStack stack) {
        if (stack.isEmpty()) return null;

        LiquefactionRecipe recipe = findByItem(stack, r -> r.inputItem[0].matchesItem(stack));
        if (recipe != null) return recipe.melt();

        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            int fill = (int) (food.saturation() * 10);
            if (fill > 0) return new FluidStackNTM(NTMFluids.SALIENT, fill);
        }
        return null;
    }
}
