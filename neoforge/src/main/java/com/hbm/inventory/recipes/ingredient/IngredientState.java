// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.world.item.ItemStack;

public interface IngredientState {
    boolean acceptsState(ItemStack stack);

    default boolean acceptsState(ItemStack stack, DataComponentMatchers additional) {
        return acceptsState(stack) || RecipeInputState.accepts(additional, stack);
    }
}
