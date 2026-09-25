// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemBatteryPack extends ItemBattery {

    public final EnumBatteryPack tier;

    public ItemBatteryPack(Properties props, EnumBatteryPack tier) {
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
                                BobMathUtil.getShortNumber(charge),
                                BobMathUtil.getShortNumber(maxCharge),
                                pct)
                        .withStyle(ChatFormatting.GREEN));
        adder.accept(
                Component.translatable(
                                "desc.shared.chargeRate", BobMathUtil.getShortNumber(chargeRate))
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable(
                                "desc.shared.dischargeRate",
                                BobMathUtil.getShortNumber(dischargeRate))
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
