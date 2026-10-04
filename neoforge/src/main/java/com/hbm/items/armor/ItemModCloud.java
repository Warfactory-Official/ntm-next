// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.lib.Library;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class ItemModCloud extends ItemArmorMod {
    public static final Identifier SPEED_ID = Library.id("armor_mod_cloud");

    private static final double SPEED = 0.125D;
    private static final int DASHES = 3;

    public ItemModCloud(Properties properties) {
        super(properties, ArmorModHandler.PLATE_ONLY, false, true, false, false);
    }

    public int dashes() {
        return DASHES;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.cloud").withStyle(ChatFormatting.WHITE));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable("desc.item.armorMod.cloud.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean hasAttributes() {
        return true;
    }

    @Override
    public void collectModifiers(
            EquipmentSlot slot,
            Identifier id,
            BiConsumer<Holder<Attribute>, AttributeModifier> adder) {
        adder.accept(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(
                        SPEED_ID, SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
