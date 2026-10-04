// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.function.Consumer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public interface ListSlotSource extends SlotSource, TrackedStorage {
    NonNullList<ItemStack> inventorySlotList();

    void inventorySlotList(NonNullList<ItemStack> items);

    @Override
    default int storedSlotCount() {
        return inventorySlotList().size();
    }

    @Override
    default boolean directStoredMutation() {
        return true;
    }

    @Override
    default ItemStack storedItem(int slot) {
        return inventorySlotList().get(slot);
    }

    @Override
    default boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (inventorySlotList().get(slot) != expected) return false;
        inventorySlotList().set(slot, replacement);
        return true;
    }

    @Override
    default void bindInventorySlots() {
        NonNullList<ItemStack> items = inventorySlotList();
        if (!(items instanceof StoredInventory tracked) || !tracked.owns(this))
            inventorySlotList(StoredInventory.track(this, items));
    }

    @Override
    default void refreshInventorySlots() {
        NonNullList<ItemStack> items = inventorySlotList();
        if (!(items instanceof StoredInventory tracked) || !tracked.owns(this)) {
            bindInventorySlots();
            InventoryChanges.layoutChanged(this);
        }
    }

    @Override
    default void inventorySources(Consumer<Object> sources) {
        sources.accept(this);
    }

    @Override
    default void refreshInventorySources() {
        refreshInventorySlots();
    }
}
