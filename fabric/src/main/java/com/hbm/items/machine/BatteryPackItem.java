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

import com.hbm.items.machine.BatteryPackType;
import com.hbm.util.NumberFormatter;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class BatteryPackItem extends BatteryItem {

    public final BatteryPackType tier;

    public BatteryPackItem(Properties props, BatteryPackType tier) {
        super(props, tier.capacity, tier.chargeRate, tier.dischargeRate);
        this.tier = tier;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) != maxCharge;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        long charge = getCharge(stack);
        double pct = charge * 1000 / maxCharge / 10D;
        adder.accept(
                Component.translatable(
                                "desc.shared.energyStoredOf",
                                NumberFormatter.formatCompact(charge),
                                NumberFormatter.formatCompact(maxCharge),
                                pct)
                        .withStyle(ChatFormatting.GREEN));
        adder.accept(
                Component.translatable(
                                "desc.shared.chargeRate", NumberFormatter.formatCompact(chargeRate))
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable(
                                "desc.shared.dischargeRate",
                                NumberFormatter.formatCompact(dischargeRate))
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable(
                                "desc.item.batteryPack.timeForFullCharge",
                                maxCharge / chargeRate / SharedConstants.TICKS_PER_SECOND / 60D)
                        .withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.translatable(
                                "desc.item.batteryPack.chargeLastsFor",
                                maxCharge / dischargeRate / SharedConstants.TICKS_PER_SECOND / 60D)
                        .withStyle(ChatFormatting.GOLD));
    }

    @Override
    protected long defaultCharge() {
        return 0L;
    }

    @Override
    public ItemStack full() {
        ItemStack stack = new ItemStack(this);
        setCharge(stack, maxCharge);
        return stack;
    }
}
