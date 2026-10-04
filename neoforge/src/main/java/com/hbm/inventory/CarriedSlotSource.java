// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class CarriedSlotSource implements SlotSource {
    private @Nullable Object owner;
    private final SlotSource source;

    public CarriedSlotSource(Object owner, SlotSource source) {
        this.owner = owner;
        this.source = source;
    }

    @Override
    public @Nullable Object inventoryOwner() {
        return owner;
    }

    @Override
    public void inventoryOwner(@Nullable Object owner) {
        assert owner == this.owner || owner == null;
        this.owner = owner;
    }

    @Override
    public int storedSlotCount() {
        return source.storedSlotCount();
    }

    @Override
    public ItemStack storedItem(int slot) {
        return source.storedItem(slot);
    }

    @Override
    public boolean physicalSlot(int slot) {
        return source.physicalSlot(slot);
    }

    @Override
    public boolean directStoredMutation() {
        return source.directStoredMutation();
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (!source.replaceStoredItem(slot, expected, replacement)) return false;
        InventorySlotBinding.bind(replacement, this, slot);
        return true;
    }

    @Override
    public void publishStoredChanges(int slot) {
        if (owner != null) ((CarriedInventoryOwner) owner).carriedInventoryChanged();
    }
}
