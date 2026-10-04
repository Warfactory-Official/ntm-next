// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.interfaces.StoredItemSlots;
import org.jspecify.annotations.Nullable;

public interface SlotSource extends StoredItemSlots {
    default @Nullable Object inventoryOwner() {
        return this;
    }

    default void inventoryOwner(@Nullable Object owner) {
        assert owner == this || owner == null;
    }

    default boolean physicalSlot(int slot) {
        return slot >= 0 && slot < storedSlotCount();
    }

    default void bindInventorySlots() {
        for (int slot = 0; slot < storedSlotCount(); slot++)
            InventorySlotBinding.bind(storedItem(slot), this, slot);
    }

    default void refreshInventorySlots() {}

    default boolean directStoredMutation() {
        return false;
    }

    default void prepareStoredSlot(int slot) {}

    default void publishStoredChanges(int slot) {}
}
