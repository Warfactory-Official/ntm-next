// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.api.energymk2.IBatteryItem;
import net.minecraft.world.item.ItemStack;

public final class ModArmorItemPowered extends ModArmorItem implements IBatteryItem {

    private final long chargeRate;

    public ModArmorItemPowered(
            Properties properties, Suit suit, long maxPower, long chargeRate, long consumption) {
        super(properties, suit, maxPower, consumption);
        this.chargeRate = chargeRate;
    }

    @Override
    public void chargeBattery(ItemStack stack, long amount) {
        setCharge(stack, getCharge(stack) + amount);
    }

    @Override
    public void dischargeBattery(ItemStack stack, long amount) {
        setCharge(stack, getCharge(stack) - amount);
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return maxPower(stack);
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return 0L;
    }
}
