// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemModRevive extends ItemArmorMod {
    public ItemModRevive(Properties properties, int durability) {
        super(properties.durability(durability), ArmorModHandler.EXTRA, false, false, true, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        if (this == ModItems.SCRUMPY.get()) {
            adder.accept(
                    Component.translatable("desc.item.modRevive.butHowDidYou")
                            .withStyle(ChatFormatting.GOLD));
            adder.accept(
                    Component.translatable("desc.item.modRevive.iWasDrunk")
                            .withStyle(ChatFormatting.RED));
        }
        if (this == ModItems.WILD_P.get()) {
            adder.accept(
                    Component.translatable("desc.item.modRevive.explosive")
                            .withStyle(ChatFormatting.DARK_GRAY)
                            .append(
                                    Component.translatable("desc.item.modRevive.reactive")
                                            .withStyle(ChatFormatting.RED))
                            .append(
                                    Component.translatable("desc.item.modRevive.plot")
                                            .withStyle(ChatFormatting.DARK_GRAY))
                            .append(
                                    Component.translatable("desc.item.modRevive.armor")
                                            .withStyle(ChatFormatting.RED)));
        }
        adder.accept(Component.empty());
        adder.accept(
                Component.literal((stack.getMaxDamage() - stack.getDamageValue()) + " revives left")
                        .withStyle(ChatFormatting.GOLD));
    }

    @Override
    protected boolean hasTooltipSpacer() {
        return false;
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.literal("  ")
                        .append(stack.getHoverName())
                        .append(
                                " ("
                                        + (stack.getMaxDamage() - stack.getDamageValue())
                                        + " revives left)")
                        .withStyle(ChatFormatting.GOLD));
    }
}
