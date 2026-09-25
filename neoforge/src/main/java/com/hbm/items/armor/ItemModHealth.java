// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
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

public class ItemModHealth extends ItemArmorMod {
    private final float health;

    public ItemModHealth(Properties properties, float health) {
        super(properties, ArmorModHandler.EXTRA, false, true, false, false);
        this.health = health;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        ChatFormatting color =
                System.currentTimeMillis() % 1000L < 500L
                        ? ChatFormatting.RED
                        : ChatFormatting.LIGHT_PURPLE;
        adder.accept(
                Component.translatable("desc.item.armorMod.health", Math.round(health * 10F) * 0.1F)
                        .withStyle(color));
        if (this == ModItems.BLACK_DIAMOND.get()) {
            adder.accept(Component.empty());
            adder.accept(
                    Component.translatable("desc.item.armorMod.blackDiamond")
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        ChatFormatting color =
                System.currentTimeMillis() % 1000L < 500L
                        ? ChatFormatting.RED
                        : ChatFormatting.LIGHT_PURPLE;
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.health.installed",
                                stack.getHoverName(),
                                Math.round(health * 10F) * 0.1F)
                        .withStyle(color));
    }

    @Override
    public void collectModifiers(
            ItemStack armor,
            Identifier id,
            BiConsumer<Holder<Attribute>, AttributeModifier> adder) {
        adder.accept(
                Attributes.MAX_HEALTH,
                new AttributeModifier(id, health, AttributeModifier.Operation.ADD_VALUE));
    }
}
