// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HazardTransformerPostCustom implements IHazardTransformer {

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
