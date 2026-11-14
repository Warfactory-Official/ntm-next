/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ArmorModItem extends Item {
    public final int type;
    public final boolean helmet;
    public final boolean chestplate;
    public final boolean leggings;
    public final boolean boots;

    public ArmorModItem(
            Properties properties,
            int type,
            boolean helmet,
            boolean chestplate,
            boolean leggings,
            boolean boots) {
        super(properties.stacksTo(1));
        this.type = type;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
    }

    private static Component indented(String translationKey) {
        return Component.literal("  ").append(Component.translatable(translationKey));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag tooltipFlag) {
        addSpecificTooltip(stack, adder);
        if (hasTooltipSpacer()) adder.accept(Component.empty());
        adder.accept(
                Component.translatable("armorMod.applicableTo")
                        .withStyle(ChatFormatting.DARK_PURPLE));
        if (helmet && chestplate && leggings && boots) {
            adder.accept(indented("armorMod.all"));
        } else {
            if (helmet) adder.accept(indented("armorMod.helmets"));
            if (chestplate) adder.accept(indented("armorMod.chestplates"));
            if (leggings) adder.accept(indented("armorMod.leggings"));
            if (boots) adder.accept(indented("armorMod.boots"));
        }
        adder.accept(
                Component.translatable("desc.item.armorMod.slot")
                        .withStyle(ChatFormatting.DARK_PURPLE));
        adder.accept(indented(slotTranslationKey()));
    }

    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {}

    protected boolean hasTooltipSpacer() {
        return true;
    }

    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(stack.getHoverName());
    }

    public boolean ticksServer() {
        return false;
    }

    public boolean ticksClient() {
        return false;
    }

    public boolean hasAttributes() {
        return false;
    }

    public void modUpdate(LivingEntity entity, ItemStack armor) {}

    public void modUpdateClient(LivingEntity entity, ItemStack armor) {}

    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        return amount;
    }

    public void collectModifiers(
            EquipmentSlot slot,
            Identifier id,
            BiConsumer<Holder<Attribute>, AttributeModifier> adder) {}

    private String slotTranslationKey() {
        return switch (type) {
            case ArmorModHandler.HELMET_ONLY -> "armorMod.type.helmet";
            case ArmorModHandler.PLATE_ONLY -> "armorMod.type.chestplate";
            case ArmorModHandler.LEGS_ONLY -> "armorMod.type.leggings";
            case ArmorModHandler.BOOTS_ONLY -> "armorMod.type.boots";
            case ArmorModHandler.SERVOS -> "armorMod.type.servo";
            case ArmorModHandler.CLADDING -> "armorMod.type.cladding";
            case ArmorModHandler.KEVLAR -> "armorMod.type.insert";
            case ArmorModHandler.EXTRA -> "armorMod.type.special";
            case ArmorModHandler.BATTERY -> "armorMod.type.battery";
            default -> throw new IllegalStateException("Unknown armor-mod slot: " + type);
        };
    }
}
