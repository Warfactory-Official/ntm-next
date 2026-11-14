/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: 2025 MrNorwood
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) 2025 MrNorwood. All rights reserved.
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

package com.hbm.hazard.type;

import com.hbm.data.RadiationData;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.HazardModifier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeToxic implements HazardKind {

    @Override
    public void onUpdate(LivingEntity target, float base, ItemStack stack) {
        double level = base;
        if (RadiationData.DISABLE_TOXIC.get()) return;

        boolean reacher = HazardHelper.isHoldingReacher(target);
        boolean hasHazmat = target instanceof Player player && ArmorUtil.checkForHazmat(player);

        boolean isUnprotected = !(hasHazmat || reacher);

        if (isUnprotected) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 110, (int) (level - 1)));
            if (level > 2) {
                target.addEffect(
                        new MobEffectInstance(
                                MobEffects.SLOWNESS, 110, (int) Math.min(4, level - 4)));
            }
            if (level > 4) {
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 110, (int) level));
            }
            if (level > 6 && target.getRandom().nextInt((int) (2000 / level)) == 0) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 110, (int) (level - 4)));
            }
        }

        if (level > 8) {
            target.addEffect(
                    new MobEffectInstance(MobEffects.MINING_FATIGUE, 110, (int) (level - 8)));
        }
        if (level > 16) {
            target.addEffect(
                    new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 110, (int) (level - 16)));
        }
    }

    @Override
    public void onDropped(ItemEntity item, float level) {}

    @Override
    public void appendTooltip(
            Player player,
            List<Component> list,
            float base,
            ItemStack stack,
            List<HazardModifier> modifiers) {
        double level = base;
        String adjectiveKey;
        if (level > 16) {
            adjectiveKey = "adjective.extreme";
        } else if (level > 8) {
            adjectiveKey = "adjective.veryhigh";
        } else if (level > 4) {
            adjectiveKey = "adjective.high";
        } else if (level > 2) {
            adjectiveKey = "adjective.medium";
        } else {
            adjectiveKey = "adjective.little";
        }

        list.add(
                Component.literal("[")
                        .append(Component.translatable(adjectiveKey))
                        .append(" ")
                        .append(Component.translatable("trait.toxic"))
                        .append("]")
                        .withStyle(ChatFormatting.GREEN));
    }
}
