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

package com.hbm.inventory.recipes.loader;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public final class NtmRecipeInput implements RecipeInput {

    private final ItemStack[] items;

    public NtmRecipeInput(int size) {
        items = new ItemStack[size];
        for (int i = 0; i < size; i++) items[i] = ItemStack.EMPTY;
    }

    public NtmRecipeInput set(int slot, ItemStack stack) {
        items[slot] = stack;
        return this;
    }

    public NtmRecipeInput setAll(ItemStack[] stacks) {
        System.arraycopy(stacks, 0, items, 0, Math.min(stacks.length, items.length));
        return this;
    }

    @Override
    public ItemStack getItem(int index) {
        return items[index];
    }

    @Override
    public int size() {
        return items.length;
    }
}
