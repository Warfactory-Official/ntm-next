// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.NtmRecipeInput;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class CentrifugeRecipes extends SimpleGenericRecipes<CentrifugeRecipe> {

    public static final CentrifugeRecipes INSTANCE = new CentrifugeRecipes();

    @Override
    protected String registryName() {
        return "centrifuge";
    }

    @Override
    protected RecipeSerializer<CentrifugeRecipe> serializer() {
        return CentrifugeRecipe.SERIALIZER;
    }

    public @Nullable CentrifugeRecipe matchFor(ItemStack input, Level level) {
        if (input.isEmpty()) return null;
        NtmRecipeInput in = new NtmRecipeInput(1).set(0, input);
        return findByItem(input, r -> r.matches(in, level));
    }

    public ItemStack @Nullable [] getOutputs(ItemStack input, Level level) {
        if (input.isEmpty()) return null;
        NtmRecipeInput in = new NtmRecipeInput(1).set(0, input);
        CentrifugeRecipe r = findByItem(input, row -> row.matches(in, level));
        if (r == null) return null;
        WeightedList<ItemStack>[] outs = r.outputItems();
        ItemStack[] result = new ItemStack[outs.length];
        for (int i = 0; i < outs.length; i++) {
            result[i] = outs[i].getRandom(RNG).orElse(ItemStack.EMPTY).copy();
        }
        return result;
    }
}
