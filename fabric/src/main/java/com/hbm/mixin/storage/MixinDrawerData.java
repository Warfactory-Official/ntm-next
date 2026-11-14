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

package com.hbm.mixin.storage;

import com.hbm.inventory.SlotSource;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.StandardDrawerGroup.DrawerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = DrawerData.class, remap = false, priority = 500)
public abstract class MixinDrawerData implements SlotSource {
    @Shadow private ItemStack protoStack;
    @Shadow private int count;
    @Shadow private boolean missing;

    @Shadow
    protected abstract void setStoredItemCount(int count, boolean notify);

    @Shadow
    protected abstract void onItemChanged();

    @Shadow
    protected abstract void onAmountChanged();

    @Shadow
    public abstract IDrawerAttributes getAttributes();

    @Shadow
    protected abstract IDrawer setStoredItem(ItemStack stack, boolean notify);

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
    public int storedSlotCount() {
        return 1;
    }

    @Override
    public ItemStack storedItem(int slot) {
        return protoStack;
    }

    @Override
    public boolean physicalSlot(int slot) {
        if (slot != 0 || count <= 0 || missing) return false;
        return !getAttributes().isUnlimitedVending();
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (protoStack != expected || !physicalSlot(slot)) return false;
        int amount = count;
        setStoredItem(replacement, false);
        if (!replacement.isEmpty()) setStoredItemCount(amount, false);
        return replacement.isEmpty() ? count == 0 : ItemStack.matches(protoStack, replacement);
    }

    @Override
    public void publishStoredChanges(int slot) {
        onItemChanged();
        onAmountChanged();
    }
}
