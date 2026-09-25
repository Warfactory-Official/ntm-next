// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
