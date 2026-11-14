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

package com.hbm.items.tool;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.items.ModDataComponents;
import com.hbm.util.NumberFormatter;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class SwordAbilityPowerItem extends SwordAbilityItem implements EnergyItem {

    private final long maxPower;
    private final long chargeRate;
    private final long consumption;

    public SwordAbilityPowerItem(
            Properties properties,
            AvailableAbilities abilities,
            long maxPower,
            long chargeRate,
            long consumption) {
        super(properties, abilities);
        this.maxPower = maxPower;
        this.chargeRate = chargeRate;
        this.consumption = consumption;
    }

    @Override
    public long getCharge(ItemStack stack) {
        return Math.clamp(
                stack.getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), maxPower), 0L, maxPower);
    }

    @Override
    public void setCharge(ItemStack stack, long charge) {
        stack.set(ModDataComponents.BATTERY_CHARGE.get(), Math.clamp(charge, 0L, maxPower));
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return maxPower;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean canOperate(ItemStack stack) {
        return getCharge(stack) >= consumption;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        super.hurtEnemy(stack, victim, attacker);
        if (!victim.level().isClientSide()) removeCharge(stack, consumption);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < maxPower;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getCharge(stack) * 13F / maxPower);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb((float) getCharge(stack) / maxPower / 3.0F, 1.0F, 1.0F);
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
                        "desc.item.toolAbility.charge",
                        NumberFormatter.formatCompact(getCharge(stack)),
                        NumberFormatter.formatCompact(maxPower)));
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
