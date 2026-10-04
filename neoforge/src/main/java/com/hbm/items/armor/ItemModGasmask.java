// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemModGasmask extends ItemArmorMod {

    private final Variant variant;

    public ItemModGasmask(Properties properties, Variant variant) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
        this.variant = variant;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.gasMask")
                        .withStyle(ChatFormatting.GREEN));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable("desc.item.armorMod.gasMask.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.GREEN));
    }

    public Variant variant() {
        return variant;
    }

    public enum Variant {
        STANDARD,
        MONO
    }
}
