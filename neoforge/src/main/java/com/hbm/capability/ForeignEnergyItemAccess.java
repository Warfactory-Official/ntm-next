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

package com.hbm.capability;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.items.machine.BatteryCreativeItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ForeignEnergyItemAccess {

    private ForeignEnergyItemAccess() {}

    public static List<Item> batteryItems() {
        List<Item> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof EnergyItem) out.add(item);
        }
        return out;
    }

    public static boolean isCreative(ItemStack stack) {
        return stack.getItem() instanceof BatteryCreativeItem;
    }

    public static long chargeHe(ItemStack stack) {
        return stack.getItem() instanceof EnergyItem battery ? battery.getCharge(stack) : 0L;
    }

    public static long capacityHe(ItemStack stack) {
        return stack.getItem() instanceof EnergyItem battery ? battery.getMaxCharge(stack) : 0L;
    }

    public static long insertableHe(ItemStack stack, long maxHe) {
        if (maxHe <= 0 || isCreative(stack)) return 0L;
        if (!(stack.getItem() instanceof EnergyItem battery)) return 0L;
        long room = battery.getMaxCharge(stack) - battery.getCharge(stack);
        return Math.max(0L, Math.min(Math.min(maxHe, battery.getChargeRate(stack)), room));
    }

    public static long extractableHe(ItemStack stack, long maxHe) {
        if (maxHe <= 0) return 0L;
        if (!(stack.getItem() instanceof EnergyItem battery)) return 0L;
        if (isCreative(stack)) return maxHe;
        return Math.max(
                0L,
                Math.min(
                        Math.min(maxHe, battery.getDischargeRate(stack)),
                        battery.getCharge(stack)));
    }

    public static ItemStack withCharge(ItemStack stack, long deltaHe) {
        ItemStack copy = stack.copyWithCount(1);
        if (!(copy.getItem() instanceof EnergyItem battery) || deltaHe == 0 || isCreative(copy))
            return copy;
        if (deltaHe > 0) {
            battery.addCharge(copy, deltaHe);
        } else {
            battery.removeCharge(copy, -deltaHe);
        }
        return copy;
    }
}
