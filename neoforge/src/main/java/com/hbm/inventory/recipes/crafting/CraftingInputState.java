/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;

public interface CraftingInputState extends CraftingRecipe {
    default @Nullable Ingredient resultCarrier() {
        return null;
    }

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
