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

package com.hbm.fabric.mixin.storage;

import com.hbm.fabric.integration.StoredItemTransactions;
import com.hbm.inventory.ListSlotSource;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import reborncore.api.items.InventoryBase;

@Mixin(value = InventoryBase.class, remap = false, priority = 500)
public abstract class MixinInventoryBase implements ListSlotSource {
    @Shadow private NonNullList<ItemStack> stacks;

    @Override
    public boolean directStoredMutation() {
        return false;
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (!StoredItemTransactions.isIdle() || stacks.get(slot) != expected) return false;
        ((Container) (Object) this).setItem(slot, replacement);
        return true;
    }
}
