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

package com.hbm.neoforge.mixin;

import com.hbm.inventory.ListSlotSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ItemStacksResourceHandler.class, priority = 500)
public abstract class MixinItemStacksResourceHandler
        extends StacksResourceHandler<ItemStack, ItemResource> implements ListSlotSource {
    protected MixinItemStacksResourceHandler() {
        super(0, ItemStack.EMPTY, ItemStack.OPTIONAL_CODEC);
    }

    @Override
    public boolean directStoredMutation() {
        return false;
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || stacks.get(slot) != expected) return false;
        ((ItemStacksResourceHandler) (Object) this)
                .set(slot, ItemResource.of(replacement), replacement.getCount());
        return ItemStack.matches(stacks.get(slot), replacement);
    }
}
