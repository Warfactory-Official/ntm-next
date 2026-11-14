/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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
import com.hbm.util.RegistryUtil;
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

public class ModCloudItem extends ArmorModItem {
    public static final Identifier SPEED_ID = RegistryUtil.id("armor_mod_cloud");

    private static final double SPEED = 0.125D;
    private static final int DASHES = 3;

    public ModCloudItem(Properties properties) {
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
