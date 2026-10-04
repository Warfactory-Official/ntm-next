// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import net.minecraft.world.item.ItemStack;

public interface InteriorSlotSource extends ListSlotSource, InventoryAlias {
    @Override
    default void refreshInventorySlots() {
        if (inventoryOwner() == null) return;
        var items = inventorySlotList();
        if (items instanceof StoredInventory tracked && tracked.owns(this)) return;
        ((ItemInteriorBackend) inventoryOwner()).changed();
        ListSlotSource.super.refreshInventorySlots();
    }

    @Override
    default Object inventoryIdentity() {
        return this;
    }

    @Override
    default boolean directStoredMutation() {
        return false;
    }

    @Override
    default boolean physicalSlot(int slot) {
        Object owner = inventoryOwner();
        return owner instanceof ItemInteriorBackend backend && backend.physicalSlot(this, slot);
    }

    @Override
    default boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        return ((ItemInteriorBackend) inventoryOwner())
                .replaceStoredItem(this, slot, expected, replacement);
    }

    @Override
    default void publishStoredChanges(int slot) {
        ((ItemInteriorBackend) inventoryOwner()).publishStoredChanges(this, slot);
    }
}
