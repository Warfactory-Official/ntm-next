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
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModCharmItem extends ArmorModItem {
    public ModCharmItem(Item.Properties properties) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, true, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.modCharm.youFeelBlessed")
                        .withStyle(ChatFormatting.AQUA));
        if (this == ModItems.PROTECTION_CHARM.get()) {
            adder.accept(
                    Component.translatable("desc.item.modCharm.divertsMeteorsAwayFrom")
                            .withStyle(ChatFormatting.AQUA));
            adder.accept(
                    Component.translatable("desc.item.modCharm.meteorsNoLongerDestroy")
                            .withStyle(ChatFormatting.AQUA));
            adder.accept(
                    Component.translatable("desc.item.modCharm.halvesBroadcasterDamage")
                            .withStyle(ChatFormatting.AQUA));
        }
        if (this == ModItems.METEOR_CHARM.get()) {
            adder.accept(
                    Component.translatable("desc.item.modCharm.disablesMeteoriteSpawning")
                            .withStyle(ChatFormatting.AQUA));
            adder.accept(
                    Component.translatable("desc.item.modCharm.negatesBroadcasterDamage")
                            .withStyle(ChatFormatting.AQUA));
        }
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
                        .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        if (!source.is(ModDamageTypes.BROADCAST)) return amount;
        if (this == ModItems.PROTECTION_CHARM.get()) return amount * 0.5F;
        if (this == ModItems.METEOR_CHARM.get()) return 0F;
        return amount;
    }
}
