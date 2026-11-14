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
