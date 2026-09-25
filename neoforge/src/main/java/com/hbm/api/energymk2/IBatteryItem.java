// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface IBatteryItem {

    static boolean isBattery(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IBatteryItem;
    }

    static ItemStack emptyBattery(ItemStack stack) {
        if (!isBattery(stack)) return ItemStack.EMPTY;
        ItemStack empty = stack.copy();
        ((IBatteryItem) empty.getItem()).setCharge(empty, 0L);
        return empty;
    }

    static ItemStack emptyBattery(Item item) {
        if (!(item instanceof IBatteryItem battery)) return ItemStack.EMPTY;
        ItemStack empty = new ItemStack(item);
        battery.setCharge(empty, 0L);
        return empty;
    }

    void chargeBattery(ItemStack stack, long amount);

    void setCharge(ItemStack stack, long amount);

    void dischargeBattery(ItemStack stack, long amount);

    long getCharge(ItemStack stack);

    long getMaxCharge(ItemStack stack);

    long getChargeRate(ItemStack stack);

    long getDischargeRate(ItemStack stack);
}
