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
