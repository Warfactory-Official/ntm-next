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

package com.hbm.api.energymk2;

import com.hbm.items.machine.BatteryCreativeItem;
import com.hbm.platform.Caps;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class ItemEnergyTransfer {

    private ItemEnergyTransfer() {}

    public static boolean isBattery(Container container, int slot) {
        if (EnergyItem.matches(container.getItem(slot))) return true;
        return Caps.findEnergyStore(container, slot) != null;
    }

    public static long insert(Container container, int slot, long maxHe, boolean simulate) {
        if (maxHe <= 0) return 0;
        ItemStack stack = container.getItem(slot);
        if (stack.getItem() instanceof BatteryCreativeItem) {
            if (!simulate) container.setChanged();
            return maxHe;
        }
        if (stack.getItem() instanceof EnergyItem battery) {
            long accepted =
                    Math.max(
                            0L,
                            Math.min(
                                    Math.min(maxHe, battery.getChargeRate(stack)),
                                    battery.getMaxCharge(stack) - battery.getCharge(stack)));
            if (accepted > 0 && !simulate) {
                battery.addCharge(stack, accepted);
                container.setChanged();
            }
            return accepted;
        }

        net.neoforged.neoforge.transfer.energy.EnergyHandler store =
                Caps.findEnergyStore(container, slot);
        if (store == null) return 0;
        long acceptedFe =
                Caps.transfer(
                        store,
                        EnergyConversion.feFromHe(maxHe),
                        EnergyConversion.feQuantum(),
                        simulate,
                        true);
        long acceptedHe = EnergyConversion.heFromFe(acceptedFe);
        assert EnergyConversion.feFromHe(acceptedHe) == acceptedFe;
        if (acceptedHe > 0 && !simulate) container.setChanged();
        return acceptedHe;
    }

    public static long extract(Container container, int slot, long maxHe, boolean simulate) {
        if (maxHe <= 0) return 0;
        ItemStack stack = container.getItem(slot);
        if (stack.getItem() instanceof BatteryCreativeItem) {
            if (!simulate) container.setChanged();
            return maxHe;
        }
        if (stack.getItem() instanceof EnergyItem battery) {
            long provided =
                    Math.max(
                            0L,
                            Math.min(
                                    Math.min(maxHe, battery.getDischargeRate(stack)),
                                    battery.getCharge(stack)));
            if (provided > 0 && !simulate) {
                battery.removeCharge(stack, provided);
                container.setChanged();
            }
            return provided;
        }

        net.neoforged.neoforge.transfer.energy.EnergyHandler store =
                Caps.findEnergyStore(container, slot);
        if (store == null) return 0;
        long providedFe =
                Caps.transfer(
                        store,
                        EnergyConversion.feFromHe(maxHe),
                        EnergyConversion.feQuantum(),
                        simulate,
                        false);
        long providedHe = EnergyConversion.heFromFe(providedFe);
        assert EnergyConversion.feFromHe(providedHe) == providedFe;
        if (providedHe > 0 && !simulate) container.setChanged();
        return providedHe;
    }
}
