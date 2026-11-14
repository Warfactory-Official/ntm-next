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

package com.hbm.inventory.recipes.ingredient;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class IngredientGroup {

    private IngredientGroup() {}

    public static boolean matches(CountIngredient[] group, ItemStack... stacks) {
        if (group.length > stacks.length) return false;
        long claimed = 0L;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            long next = claim(group, claimed, stack);
            if (next == claimed) return false;
            claimed = next;
        }
        return Long.bitCount(claimed) == group.length;
    }

    public static boolean matches(CountIngredient[] group, Container inv, int[] slots) {
        if (group.length > slots.length) return false;
        long claimed = 0L;
        for (int slot : slots) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            long next = claim(group, claimed, stack);
            if (next == claimed) return false;
            claimed = next;
        }
        return Long.bitCount(claimed) == group.length;
    }

    public static boolean matches(CountIngredient[] group, ItemStack stack) {
        if (stack.isEmpty()) return group.length == 0;
        return group.length == 1 && group[0].test(stack);
    }

    private static long claim(CountIngredient[] group, long claimed, ItemStack stack) {
        for (int i = 0; i < group.length; i++) {
            long bit = 1L << i;
            if ((claimed & bit) == 0L && group[i].test(stack)) return claimed | bit;
        }
        return claimed;
    }
}
