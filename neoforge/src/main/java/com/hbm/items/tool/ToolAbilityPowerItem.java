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

package com.hbm.items.tool;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.items.ModDataComponents;
import com.hbm.util.NumberFormatter;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ToolAbilityPowerItem extends ToolAbilityItem implements EnergyItem {

    private final long maxPower;
    private final long chargeRate;
    private final long consumption;

    public ToolAbilityPowerItem(
            Properties properties,
            AvailableAbilities abilities,
            boolean rockBreaker,
            long maxPower,
            long chargeRate,
            long consumption) {
        this(properties, abilities, rockBreaker, false, maxPower, chargeRate, consumption);
    }

    public ToolAbilityPowerItem(
            Properties properties,
            AvailableAbilities abilities,
            boolean rockBreaker,
            boolean shears,
            long maxPower,
            long chargeRate,
            long consumption) {
        super(properties, abilities, rockBreaker, shears);
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
    protected void spendPerBlock(ItemStack stack, LivingEntity user) {
        removeCharge(stack, consumption);
    }

    @Override
    protected void spendToolAction(ItemStack stack, LivingEntity user) {
        spendPerBlock(stack, user);
    }

    @Override
    public boolean mineBlock(
            ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F)
            spendPerBlock(stack, owner);
        return true;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        super.hurtEnemy(stack, victim, attacker);
        if (!victim.level().isClientSide()) spendPerBlock(stack, attacker);
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
