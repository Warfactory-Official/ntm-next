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

package com.hbm.neoforge.mixin.storage;

import appeng.blockentity.inventory.AppEngCellInventory;
import appeng.util.inv.AppEngInternalInventory;
import com.hbm.inventory.SlotSource;
import com.hbm.inventory.StoredInventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AppEngCellInventory.class, remap = false, priority = 500)
public abstract class MixinAppEngCellInventory implements SlotSource {
    @Shadow @Final private AppEngInternalInventory inv;

    @Shadow
    public abstract int size();

    @Shadow
    public abstract ItemStack getStackInSlot(int slot);

    @Shadow
    public abstract void setItemDirect(int slot, ItemStack stack);

    @Override
    public int storedSlotCount() {
        return size();
    }

    @Override
    public ItemStack storedItem(int slot) {
        return getStackInSlot(slot);
    }

    @Override
    public void bindInventorySlots() {
        var items = ((com.hbm.inventory.ListSlotSource) (inv)).inventorySlotList();
        if (!(items instanceof StoredInventory watched) || !watched.owns(this))
            ((com.hbm.inventory.ListSlotSource) (inv))
                    .inventorySlotList(StoredInventory.track(this, items));
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || getStackInSlot(slot) != expected) return false;
        setItemDirect(slot, replacement);
        return true;
    }
}
