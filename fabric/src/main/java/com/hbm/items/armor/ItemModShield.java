// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemModShield extends ItemArmorMod {

    public final float shield;

    public ItemModShield(Properties properties, float shield) {
        super(properties, ArmorModHandler.KEVLAR, false, true, false, false);
        this.shield = shield;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        ChatFormatting color =
                System.currentTimeMillis() % 1000L < 500L
                        ? ChatFormatting.YELLOW
                        : ChatFormatting.GOLD;
        adder.accept(
                Component.translatable("desc.item.armorMod.shield", Math.round(shield * 10F) * 0.1F)
                        .withStyle(color));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        ChatFormatting color =
                System.currentTimeMillis() % 1000L < 500L
                        ? ChatFormatting.YELLOW
                        : ChatFormatting.GOLD;

        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.health.installed",
                                stack.getHoverName(),
                                Math.round(shield * 10F) * 0.1F)
                        .withStyle(color));
    }
}
