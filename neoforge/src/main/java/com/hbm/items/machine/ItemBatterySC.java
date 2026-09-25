// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemBatterySC extends ItemBattery {

    public final EnumBatterySC tier;

    public ItemBatterySC(Properties props, EnumBatterySC tier) {
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
                                    BobMathUtil.getShortNumber(tier.power))
                            .withStyle(ChatFormatting.YELLOW));
        }
        for (String line : I18nUtil.loreLines("item.hbm.battery_sc.desc")) {
            adder.accept(Component.literal(line).withStyle(ChatFormatting.RED));
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
