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

import com.hbm.extprop.PlayerState;
import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ModCardItem extends ArmorModItem {

    private final boolean queen;

    public ModCardItem(Properties properties, boolean queen) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, true, false, false);
        this.queen = queen;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable(
                                queen
                                        ? "desc.item.armorMod.cardQos.1"
                                        : "desc.item.armorMod.cardAos.1")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable(
                                queen
                                        ? "desc.item.armorMod.cardQos.2"
                                        : "desc.item.armorMod.cardAos.2")
                        .withStyle(ChatFormatting.RED));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(stack.getHoverName().copy().withStyle(ChatFormatting.RED));
    }

    @Override
    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        if (queen && entity.getRandom().nextInt(3) == 0 && entity instanceof Player player) {
            PlayerState.plink(
                    player,
                    SoundEvents.ITEM_BREAK.value(),
                    0.5F,
                    1.0F + entity.getRandom().nextFloat() * 0.5F);
            return 0;
        }
        return amount;
    }
}
