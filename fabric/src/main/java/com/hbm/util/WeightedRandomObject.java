// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import java.util.List;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;

public class WeightedRandomObject {

    public static final int PRIME = 137;

    private WeightedRandomObject() {}

    public static ItemStack pickDeterministic(WeightedList<ItemStack> outputs, int[] cursor) {

        List<Weighted<ItemStack>> entries = outputs.unwrap();

        int totalWeight = 0;
        for (Weighted<ItemStack> weighted : entries) totalWeight += weighted.weight();

        cursor[0] %= Math.max(totalWeight, 1);

        ItemStack result = null;
        int weight = 0;

        for (Weighted<ItemStack> weighted : entries) {
            weight += weighted.weight();

            if (cursor[0] < weight) {
                result = weighted.value().copy();
                break;
            }
        }

        cursor[0] += PRIME;

        return result;
    }
}
