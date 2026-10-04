// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.jspecify.annotations.Nullable;

public interface CraftingInputState extends CraftingRecipe {
    default boolean permitsInputStates(
            CraftingInput input, @Nullable ItemStack output, boolean returnsRemainders) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || stack.getComponentsPatch().isEmpty()) continue;
            if (output == null) output = assemble(input);
            if (!permitsInputState(input, slot, output, returnsRemainders)) return false;
        }
        return true;
    }

    default boolean acceptsInputState(CraftingInput input, int slot, ItemStack output) {
        return false;
    }

    default boolean permitsInputState(
            CraftingInput input, int slot, ItemStack output, boolean returnsRemainders) {
        ItemStack stack = input.getItem(slot);
        if (returnsRemainders && RecipeInputState.preservesRemainder(stack)) return true;
        if (acceptsInputState(input, slot, output)) return true;
        for (var ingredient : placementInfo().ingredients()) {
            if (ingredient.test(stack) && RecipeInputState.accepts(ingredient, stack)) return true;
        }
        return false;
    }
}
