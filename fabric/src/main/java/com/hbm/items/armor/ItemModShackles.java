// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemModShackles extends ItemArmorMod {

    public ItemModShackles(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, false, false, true, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.shackles.1")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable("desc.item.armorMod.shackles.2")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable("desc.item.armorMod.shackles.3")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        adder.accept(Component.empty());
        adder.accept(
                Component.translatable("desc.item.armorMod.shackles.revives")
                        .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.shackles.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.GOLD));
    }
}
