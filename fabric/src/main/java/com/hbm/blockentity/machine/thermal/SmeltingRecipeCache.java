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

package com.hbm.blockentity.machine.thermal;

import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.jspecify.annotations.Nullable;

final class SmeltingRecipeCache {
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> check =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private @Nullable RecipeManager manager;
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private @Nullable RecipeHolder<SmeltingRecipe> recipe;

    ItemStack result(ServerLevel level, ItemStack stack) {
        var current = level.recipeAccess();
        if (manager != current || !ItemStack.isSameItemSameComponents(input, stack)) {
            manager = current;
            input = stack.copy();
            recipe =
                    stack.isEmpty()
                            ? null
                            : check.getRecipeFor(new SingleRecipeInput(stack), level)
                                    .filter(
                                            row ->
                                                    RecipeInputState.accepts(
                                                            row.value().input(), stack))
                                    .orElse(null);
            output =
                    recipe == null
                            ? ItemStack.EMPTY
                            : recipe.value().assemble(new SingleRecipeInput(stack));
        }
        return output;
    }

    boolean accepts(ServerLevel level, ItemStack stack) {
        return !result(level, stack).isEmpty();
    }

    @Nullable RecipeHolder<SmeltingRecipe> recipe() {
        return recipe;
    }

    static boolean fits(ItemStack stored, ItemStack result, int limit) {
        return !result.isEmpty()
                && (stored.isEmpty()
                        || ItemStack.isSameItemSameComponents(stored, result)
                                && stored.getCount() + result.getCount()
                                        <= Math.min(limit, stored.getMaxStackSize()));
    }

    static void insert(NonNullList<ItemStack> inventory, int slot, ItemStack result) {
        var stored = inventory.get(slot);
        if (stored.isEmpty()) inventory.set(slot, result.copy());
        else stored.grow(result.getCount());
    }

    static void consumeFuel(NonNullList<ItemStack> inventory, int slot) {
        var fuel = inventory.get(slot);
        var remainder = fuel.getCraftingRemainder();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            inventory.set(slot, remainder == null ? ItemStack.EMPTY : remainder.create());
        }
    }
}
