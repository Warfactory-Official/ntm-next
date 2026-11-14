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

package com.hbm.items.machine;

import com.hbm.items.machine.SelfChargingBatteryType;
import com.hbm.util.NumberFormatter;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class BatterySCItem extends BatteryItem {

    public final SelfChargingBatteryType tier;

    public BatterySCItem(Properties props, SelfChargingBatteryType tier) {
        super(props, tier.power, 0L, tier.power);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (tier.power > 0) {
            adder.accept(
                    Component.translatable(
                                    "desc.shared.dischargeRate",
                                    NumberFormatter.formatCompact(tier.power))
                            .withStyle(ChatFormatting.YELLOW));
        }
        for (Component line :
                TooltipStyle.lines(
                        Component.translatableWithFallback("item.hbm.battery_sc.desc", ""))) {
            adder.accept(line.copy().withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public long getCharge(ItemStack stack) {
        return maxCharge;
    }

    @Override
    public void setCharge(ItemStack stack, long charge) {}

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }
}
