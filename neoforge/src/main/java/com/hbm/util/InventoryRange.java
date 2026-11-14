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

package com.hbm.util;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public record InventoryRange(int first, int last) {
    public int place(List<ItemStack> slots, ItemStack like, int count, boolean simulate) {
        int limit = like.getMaxStackSize();
        int placed = 0;
        for (int i = first; i <= last && placed < count; i++) {
            ItemStack slot = slots.get(i);
            boolean empty = slot.isEmpty();
            if (!empty && !ItemStack.isSameItemSameComponents(slot, like)) continue;
            int add = Math.min(count - placed, limit - slot.getCount());
            if (!simulate) {
                if (empty) slots.set(i, like.copyWithCount(add));
                else slot.grow(add);
            }
            placed += add;
        }
        return placed;
    }

    public boolean insertWhole(List<ItemStack> slots, ItemStack stack) {
        int count = stack.getCount();
        if (place(slots, stack, count, true) < count) return false;
        place(slots, stack, count, false);
        return true;
    }
}
