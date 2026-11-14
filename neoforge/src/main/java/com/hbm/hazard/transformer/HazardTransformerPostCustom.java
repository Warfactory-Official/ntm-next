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

package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HazardTransformerPostCustom implements HazardTransformer {

    private static final Map<Item, List<BiConsumer<ItemStack, List<HazardEntry>>>> ITEM_POST =
            new HashMap<>();

    public static void register(Item item, BiConsumer<ItemStack, List<HazardEntry>> hook) {
        ITEM_POST.computeIfAbsent(item, k -> new ArrayList<>()).add(hook);
    }

    @Override
    public boolean appliesTo(ItemStack stack) {
        return !ITEM_POST.isEmpty() && ITEM_POST.containsKey(stack.getItem());
    }

    @Override
    public void transform(ItemStack stack, List<HazardEntry> entries) {
        for (BiConsumer<ItemStack, List<HazardEntry>> hook : ITEM_POST.get(stack.getItem())) {
            hook.accept(stack, entries);
        }
    }
}
