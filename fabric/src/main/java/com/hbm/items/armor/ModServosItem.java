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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class ModServosItem extends ArmorModItem {

    public enum Tier {
        STANDARD(0, 1, 0.5D, 0.25D, "standard"),
        DESH(2, 2, 1.5D, 0.5D, "desh");

        final int haste;
        final int jump;
        final double damage;
        final double speed;
        final String key;

        Tier(int haste, int jump, double damage, double speed, String key) {
            this.haste = haste;
            this.jump = jump;
            this.damage = damage;
            this.speed = speed;
            this.key = key;
        }
    }

    private final Tier tier;

    public ModServosItem(Properties properties, Tier tier) {
        super(properties, ArmorModHandler.SERVOS, false, true, true, false);
        this.tier = tier;
    }

    private static EquipmentSlot slotOf(ItemStack armor) {
        var equippable = armor.get(DataComponents.EQUIPPABLE);
        return equippable == null ? EquipmentSlot.MAINHAND : equippable.slot();
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.servos." + tier.key + ".chest")
                        .withStyle(ChatFormatting.DARK_PURPLE));
        adder.accept(
                Component.translatable("desc.item.armorMod.servos." + tier.key + ".legs")
                        .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        String half =
                switch (slotOf(armor)) {
                    case CHEST -> "chest";
                    case LEGS -> "legs";
                    default -> null;
                };
        if (half == null) return;
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.servos." + tier.key + "." + half + ".installed",
                                stack.getHoverName())
                        .withStyle(ChatFormatting.DARK_PURPLE));
    }

    public int effectAmplifier(EquipmentSlot slot) {
        return switch (slot) {
            case CHEST -> tier.haste;
            case LEGS -> tier.jump;
            default -> -1;
        };
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
        switch (slot) {
            case CHEST ->
                    adder.accept(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(
                                    id,
                                    tier.damage,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            case LEGS ->
                    adder.accept(
                            Attributes.MOVEMENT_SPEED,
                            new AttributeModifier(
                                    id,
                                    tier.speed,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            default -> {}
        }
    }
}
