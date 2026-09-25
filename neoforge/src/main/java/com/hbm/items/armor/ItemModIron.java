// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class ItemModIron extends ItemArmorMod {

    private static final double KNOCKBACK = 0.5D;

    public ItemModIron(Properties properties) {
        super(properties, ArmorModHandler.CLADDING, true, true, true, true);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.knockback", KNOCKBACK)
                        .withStyle(ChatFormatting.WHITE));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.knockback.installed",
                                stack.getHoverName(),
                                KNOCKBACK)
                        .withStyle(ChatFormatting.WHITE));
    }

    @Override
    public void collectModifiers(
            ItemStack armor,
            Identifier id,
            BiConsumer<Holder<Attribute>, AttributeModifier> adder) {
        adder.accept(
                Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(id, KNOCKBACK, AttributeModifier.Operation.ADD_VALUE));
    }
}
