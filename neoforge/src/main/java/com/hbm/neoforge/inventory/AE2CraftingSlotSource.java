// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.inventory;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.crafting.inv.ListCraftingInventory;
import com.hbm.handler.UnstableFuses;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.ItemInteriors;
import com.hbm.inventory.KeyedSlotSource;
import java.util.Arrays;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public interface AE2CraftingSlotSource extends KeyedSlotSource {
    @Nullable ListCraftingInventory craftingInventory();

    boolean craftingInventoryResident();

    void publishCraftingInventory();

    @Override
    default int storedSlotCount() {
        return inventorySlotKeys().length;
    }

    @Override
    default void bindInventorySlots() {
        inventoryKeySlots().clear();
        inventoryFreeSlots().clear();
        inventorySlotKeys(new Object[0]);
        ListCraftingInventory inventory = craftingInventory();
        if (inventory != null) for (var entry : inventory.list) craftingKeyChanged(entry.getKey());
    }

    default void craftingKeyChanged(AEKey key) {
        if (inventoryOwner() == null) return;
        var slots = inventoryKeySlots();
        int slot = slots.getInt(key);
        ListCraftingInventory inventory = craftingInventory();
        boolean candidate =
                inventory != null
                        && inventory.list.get(key) > 0
                        && key instanceof AEItemKey item
                        && (UnstableFuses.deadline(item.getReadOnlyStack()) != Long.MAX_VALUE
                                || ItemInteriors.hasReferences(item.getReadOnlyStack()));
        if (!candidate) {
            if (slot < 0) return;
            slots.removeInt(key);
            inventorySlotKeys()[slot] = null;
            inventoryFreeSlots().add(slot);
        } else if (slot < 0) {
            var free = inventoryFreeSlots();
            if (free.isEmpty()) {
                Object[] old = inventorySlotKeys();
                Object[] expanded = Arrays.copyOf(old, Math.max(4, old.length * 2));
                inventorySlotKeys(expanded);
                for (int i = expanded.length - 1; i >= old.length; i--) free.add(i);
            }
            slot = free.removeInt(free.size() - 1);
            slots.put(key, slot);
            inventorySlotKeys()[slot] = key;
        }
        InventoryChanges.slotChanged(this, slot);
    }

    @Override
    default boolean physicalSlot(int slot) {
        Object[] keys = inventorySlotKeys();
        ListCraftingInventory inventory = craftingInventory();
        return slot >= 0
                && slot < keys.length
                && keys[slot] != null
                && inventory != null
                && inventory.list.get((AEKey) keys[slot]) > 0;
    }

    @Override
    default ItemStack storedItem(int slot) {
        return physicalSlot(slot)
                ? ((AEItemKey) inventorySlotKeys()[slot]).getReadOnlyStack()
                : ItemStack.EMPTY;
    }

    @Override
    default boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (!craftingInventoryResident()
                || Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || !physicalSlot(slot)
                || storedItem(slot) != expected) return false;
        assert replacement.isEmpty() || replacement.getCount() == expected.getCount();
        AEKey key = (AEKey) inventorySlotKeys()[slot];
        ListCraftingInventory inventory = craftingInventory();
        long amount = inventory.list.get(key);
        long extracted = inventory.extract(key, amount, Actionable.MODULATE);
        assert extracted == amount;
        if (!replacement.isEmpty())
            inventory.insert(AEItemKey.of(replacement), amount, Actionable.MODULATE);
        return true;
    }

    @Override
    default void publishStoredChanges(int slot) {
        publishCraftingInventory();
    }
}
