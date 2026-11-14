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

package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import com.hbm.extprop.LivingHazards;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.Exposure;
import com.hbm.hazard.modifier.HazardModifier;
import com.hbm.util.ArmorRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeCoal implements HazardKind {

    @Override
    public void onUpdate(LivingEntity target, float base, ItemStack stack) {
        double level = base;
        if (RadiationData.DISABLE_COAL.get()) return;
        if (!ArmorRegistry.hasProtection(target, EquipmentSlot.HEAD, Exposure.PARTICLE_COARSE)) {
            LivingHazards.addBlackLung(
                    target,
                    (int) Math.min(level * stack.getCount(), 10) * RadiationConfig.hazardRate);
        } else if (target.getRandom().nextInt(Math.max(65 - stack.getCount(), 1)) == 0) {
            ArmorUtil.damageGasMaskFilter(target, (int) level * RadiationConfig.hazardRate);
        }
    }

    @Override
    public void onDropped(ItemEntity item, float level) {}

    @Override
    public void appendTooltip(
            Player player,
            List<Component> list,
            float level,
            ItemStack stack,
            List<HazardModifier> modifiers) {
        list.add(
                Component.literal("[")
                        .append(Component.translatable("trait.coal"))
                        .append("]")
                        .withStyle(ChatFormatting.DARK_GRAY));
    }
}
