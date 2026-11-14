/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.machine;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.items.ModDataComponents;
import com.hbm.util.NumberFormatter;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class BatteryItem extends Item implements EnergyItem {

    public final long maxCharge;
    public final long chargeRate;
    public final long dischargeRate;

    public BatteryItem(Properties props, long maxCharge, long chargeRate, long dischargeRate) {
        super(props);
        this.maxCharge = maxCharge;
        this.chargeRate = chargeRate;
        this.dischargeRate = dischargeRate;
    }

    @Override
    public long getCharge(ItemStack stack) {
        long charge = stack.getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), defaultCharge());
        return Math.max(0L, Math.min(charge, maxCharge));
    }

    protected long defaultCharge() {
        return maxCharge;
    }

    @Override
    public void setCharge(ItemStack stack, long charge) {
        stack.set(
                ModDataComponents.BATTERY_CHARGE.get(), Math.max(0L, Math.min(charge, maxCharge)));
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return maxCharge;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return dischargeRate;
    }

    public ItemStack full() {
        return new ItemStack(this);
    }

    public ItemStack empty() {
        ItemStack stack = new ItemStack(this);
        stack.set(ModDataComponents.BATTERY_CHARGE.get(), 0L);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getCharge(stack) * 13F / maxCharge);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb((float) getCharge(stack) / maxCharge / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                        "desc.shared.energyStored",
                        NumberFormatter.formatCompact(getCharge(stack))
                                + "/"
                                + NumberFormatter.formatCompact(maxCharge)
                                + "HE"));
        adder.accept(
                Component.translatable(
                        "desc.shared.chargeRate", NumberFormatter.formatCompact(chargeRate)));
        adder.accept(
                Component.translatable(
                        "desc.shared.dischargeRate", NumberFormatter.formatCompact(dischargeRate)));
    }
}
