// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.machine.ItemBatteryCreative;
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
            if (item instanceof IBatteryItem) out.add(item);
        }
        return out;
    }

    public static boolean isCreative(ItemStack stack) {
        return stack.getItem() instanceof ItemBatteryCreative;
    }

    public static long chargeHe(ItemStack stack) {
        return stack.getItem() instanceof IBatteryItem battery ? battery.getCharge(stack) : 0L;
    }

    public static long capacityHe(ItemStack stack) {
        return stack.getItem() instanceof IBatteryItem battery ? battery.getMaxCharge(stack) : 0L;
    }

    public static long insertableHe(ItemStack stack, long maxHe) {
        if (maxHe <= 0 || isCreative(stack)) return 0L;
        if (!(stack.getItem() instanceof IBatteryItem battery)) return 0L;
        long room = battery.getMaxCharge(stack) - battery.getCharge(stack);
        return Math.max(0L, Math.min(Math.min(maxHe, battery.getChargeRate(stack)), room));
    }

    public static long extractableHe(ItemStack stack, long maxHe) {
        if (maxHe <= 0) return 0L;
        if (!(stack.getItem() instanceof IBatteryItem battery)) return 0L;

        if (isCreative(stack)) return maxHe;
        return Math.max(
                0L,
                Math.min(
                        Math.min(maxHe, battery.getDischargeRate(stack)),
                        battery.getCharge(stack)));
    }

    public static ItemStack withCharge(ItemStack stack, long deltaHe) {
        ItemStack copy = stack.copyWithCount(1);
        if (!(copy.getItem() instanceof IBatteryItem battery) || deltaHe == 0 || isCreative(copy))
            return copy;
        if (deltaHe > 0) {
            battery.chargeBattery(copy, deltaHe);
        } else {
            battery.dischargeBattery(copy, -deltaHe);
        }
        return copy;
    }
}
