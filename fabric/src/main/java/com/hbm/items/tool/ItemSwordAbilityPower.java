// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.items.ModDataComponents;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemSwordAbilityPower extends ItemSwordAbility implements IBatteryItem {

    private final long maxPower;
    private final long chargeRate;
    private final long consumption;

    public ItemSwordAbilityPower(
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
    public void chargeBattery(ItemStack stack, long amount) {
        setCharge(stack, getCharge(stack) + amount);
    }

    @Override
    public void dischargeBattery(ItemStack stack, long amount) {
        setCharge(stack, getCharge(stack) - amount);
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
        if (!victim.level().isClientSide()) dischargeBattery(stack, consumption);
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
                        BobMathUtil.getShortNumber(getCharge(stack)),
                        BobMathUtil.getShortNumber(maxPower)));
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
