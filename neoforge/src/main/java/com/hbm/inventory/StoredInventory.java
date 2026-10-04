// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public class StoredInventory extends NonNullList<ItemStack> {
    private final Object owner;

    protected StoredInventory(Object owner, List<ItemStack> items) {
        super(items, ItemStack.EMPTY);
        this.owner = owner;
        for (int slot = 0; slot < items.size(); slot++)
            InventorySlotBinding.bind(items.get(slot), owner, slot);
    }

    public static NonNullList<ItemStack> create(Object owner, int size) {
        ItemStack[] items = new ItemStack[size];
        Arrays.fill(items, ItemStack.EMPTY);
        return new StoredInventory(owner, Arrays.asList(items));
    }

    public static NonNullList<ItemStack> track(Object owner, NonNullList<ItemStack> source) {
        return new StoredInventory(owner, Arrays.asList(source.toArray(ItemStack[]::new)));
    }

    public boolean owns(Object source) {
        return owner == source;
    }

    @Override
    public ItemStack set(int index, ItemStack item) {
        ItemStack previous = super.set(index, item);
        if (previous != item) {
            InventorySlotBinding.unbind(previous, owner, index);
            InventorySlotBinding.bind(item, owner, index);
            InventoryChanges.slotChanged(owner, index);
        }
        return previous;
    }

    @Override
    public void clear() {
        for (int slot = 0; slot < size(); slot++) set(slot, ItemStack.EMPTY);
    }
}
