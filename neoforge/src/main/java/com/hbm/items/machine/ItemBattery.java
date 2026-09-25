// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.ModDataComponents;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemBattery extends Item implements IBatteryItem {

    public final long maxCharge;
    public final long chargeRate;
    public final long dischargeRate;

    public ItemBattery(Properties props, long maxCharge, long chargeRate, long dischargeRate) {
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
    public void chargeBattery(ItemStack stack, long amount) {
        setCharge(stack, getCharge(stack) + amount);
    }

    @Override
    public void setCharge(ItemStack stack, long charge) {
        stack.set(
                ModDataComponents.BATTERY_CHARGE.get(), Math.max(0L, Math.min(charge, maxCharge)));
    }

    @Override
    public void dischargeBattery(ItemStack stack, long amount) {
        setCharge(stack, getCharge(stack) - amount);
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
                        BobMathUtil.getShortNumber(getCharge(stack))
                                + "/"
                                + BobMathUtil.getShortNumber(maxCharge)
                                + "HE"));
        adder.accept(
                Component.translatable(
                        "desc.shared.chargeRate", BobMathUtil.getShortNumber(chargeRate)));
        adder.accept(
                Component.translatable(
                        "desc.shared.dischargeRate", BobMathUtil.getShortNumber(dischargeRate)));
    }
}
