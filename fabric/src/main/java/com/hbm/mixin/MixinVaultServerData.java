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

package com.hbm.mixin;

import com.hbm.inventory.SlotList;
import com.hbm.inventory.SlotSource;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VaultServerData.class)
public abstract class MixinVaultServerData implements SlotSource {
    @Shadow @Final @Mutable private List<ItemStack> itemsToEject;
    @Shadow boolean isDirty;
    @Unique private @Nullable Object hbm$owner;

    @Override
    public @Nullable Object inventoryOwner() {
        return hbm$owner;
    }

    @Override
    public void inventoryOwner(@Nullable Object owner) {
        hbm$owner = owner;
    }

    @Override
    public void bindInventorySlots() {
        if (!(itemsToEject instanceof SlotList)) itemsToEject = new SlotList(this, itemsToEject);
    }

    @Override
    public int storedSlotCount() {
        return itemsToEject.size();
    }

    @Override
    public ItemStack storedItem(int slot) {
        return itemsToEject.get(slot);
    }

    @Override
    public boolean directStoredMutation() {
        return true;
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (itemsToEject.get(slot) != expected) return false;
        itemsToEject.set(slot, replacement);
        isDirty = true;
        return true;
    }

    @Override
    public void publishStoredChanges(int slot) {
        itemsToEject.removeIf(ItemStack::isEmpty);
    }
}
