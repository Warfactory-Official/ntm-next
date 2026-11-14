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

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.hbm.inventory.SlotSource;
import com.hbm.neoforge.inventory.AE2SendList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PatternProviderLogic.class, remap = false, priority = 500)
public abstract class MixinPatternProviderLogic implements SlotSource {
    @Shadow @Final @Mutable private List<GenericStack> sendList;
    @Shadow @Final private PatternProviderLogicHost host;

    @Override
    public void bindInventorySlots() {
        if (!(sendList instanceof AE2SendList watched) || !watched.owns(this))
            sendList = new AE2SendList(this, sendList);
    }

    @Override
    public int storedSlotCount() {
        return sendList.size();
    }

    @Override
    public boolean physicalSlot(int slot) {
        return slot >= 0
                && slot < sendList.size()
                && sendList.get(slot).amount() > 0
                && sendList.get(slot).what() instanceof AEItemKey;
    }

    @Override
    public ItemStack storedItem(int slot) {
        GenericStack held = sendList.get(slot);
        return held.amount() > 0 && held.what() instanceof AEItemKey item
                ? item.getReadOnlyStack()
                : ItemStack.EMPTY;
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || !physicalSlot(slot)
                || storedItem(slot) != expected) return false;
        assert replacement.isEmpty() || replacement.getCount() == expected.getCount();
        GenericStack held = sendList.get(slot);
        sendList.set(
                slot,
                replacement.isEmpty()
                        ? new GenericStack(held.what(), 0)
                        : new GenericStack(AEItemKey.of(replacement), held.amount()));
        return true;
    }

    @Override
    public void publishStoredChanges(int slot) {
        sendList.removeIf(held -> held.amount() == 0);
        host.saveChanges();
    }
}
