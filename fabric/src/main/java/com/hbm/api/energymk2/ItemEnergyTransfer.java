// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.items.machine.ItemBatteryCreative;
import com.hbm.platform.IEnergyHandlerView;
import com.hbm.platform.Services;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class ItemEnergyTransfer {

    private ItemEnergyTransfer() {}

    public static long insert(Container container, int slot, long maxHe, boolean simulate) {
        if (maxHe <= 0) return 0;
        ItemStack stack = container.getItem(slot);
        if (stack.getItem() instanceof ItemBatteryCreative) return maxHe;
        if (stack.getItem() instanceof IBatteryItem battery) {
            long accepted =
                    Math.max(
                            0L,
                            Math.min(
                                    Math.min(maxHe, battery.getChargeRate(stack)),
                                    battery.getMaxCharge(stack) - battery.getCharge(stack)));
            if (accepted > 0 && !simulate) {
                battery.chargeBattery(stack, accepted);
                container.setChanged();
            }
            return accepted;
        }

        IEnergyHandlerView view = Services.CAPS.findEnergyHandler(container, slot);
        if (view == null) return 0;
        long acceptedFe =
                view.insert(
                        EnergyConversion.feFromHe(maxHe), EnergyConversion.feQuantum(), simulate);
        long acceptedHe = EnergyConversion.heFromFe(acceptedFe);
        assert EnergyConversion.feFromHe(acceptedHe) == acceptedFe;
        if (acceptedHe > 0 && !simulate) container.setChanged();
        return acceptedHe;
    }

    public static long extract(Container container, int slot, long maxHe, boolean simulate) {
        if (maxHe <= 0) return 0;
        ItemStack stack = container.getItem(slot);
        if (stack.getItem() instanceof ItemBatteryCreative) return maxHe;
        if (stack.getItem() instanceof IBatteryItem battery) {
            long provided =
                    Math.max(
                            0L,
                            Math.min(
                                    Math.min(maxHe, battery.getDischargeRate(stack)),
                                    battery.getCharge(stack)));
            if (provided > 0 && !simulate) {
                battery.dischargeBattery(stack, provided);
                container.setChanged();
            }
            return provided;
        }

        IEnergyHandlerView view = Services.CAPS.findEnergyHandler(container, slot);
        if (view == null) return 0;
        long providedFe =
                view.extract(
                        EnergyConversion.feFromHe(maxHe), EnergyConversion.feQuantum(), simulate);
        long providedHe = EnergyConversion.heFromFe(providedFe);
        assert EnergyConversion.feFromHe(providedHe) == providedFe;
        if (providedHe > 0 && !simulate) container.setChanged();
        return providedHe;
    }
}
