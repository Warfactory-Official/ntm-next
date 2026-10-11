// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class ItemNo9 extends ModArmorItem {

    public ItemNo9(Properties properties) {
        super(properties, Suit.NONE);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable("desc.item.armor.damageThreshold", 0.5F)
                        .withStyle(ChatFormatting.BLUE));
        adder.accept(
                Component.translatable("desc.item.no9.breatheCoal")
                        .withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
