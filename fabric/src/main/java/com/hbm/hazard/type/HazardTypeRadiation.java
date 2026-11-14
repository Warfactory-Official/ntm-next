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

package com.hbm.hazard.type;

import com.hbm.config.BalanceConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.HazardModifier;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.NtmMath;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HazardTypeRadiation implements HazardKind {
    public static double getNewValue(double radiation) {
        if (radiation < 1_000_000D) return radiation;
        if (radiation < 1_000_000_000D) return radiation * 0.000001D;
        return radiation * 0.000000001D;
    }

    private static String getSuffixKey(double radiation) {
        if (radiation < 1_000_000D) return null;
        if (radiation < 1_000_000_000D) return "desc.mil";
        return "desc.bil";
    }

    public static void appendRadiationLines(List<Component> list, double perItem, int count) {
        list.add(
                Component.literal("[")
                        .append(Component.translatable("trait.radioactive"))
                        .append("]")
                        .withStyle(ChatFormatting.GREEN));
        list.add(radLine(perItem, null));
        if (count > 1) list.add(radLine(perItem * count, "desc.stack"));
    }

    private static Component radLine(double rads, @Nullable String prefixKey) {
        String suffixKey = getSuffixKey(rads);
        String value = " " + NtmMath.roundFloat(getNewValue(rads), 3);
        MutableComponent line =
                prefixKey == null
                        ? Component.literal(value)
                        : Component.empty()
                                .append(" ")
                                .append(Component.translatable(prefixKey))
                                .append(value);
        if (suffixKey != null) line.append(Component.translatable(suffixKey));
        return line.append(" ")
                .append(Component.translatable("desc.rads"))
                .withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public void onUpdate(LivingEntity target, float base, ItemStack stack) {
        double level = base;
        boolean reacher = HazardHelper.isHoldingReacher(target);
        level *= stack.getCount();
        if (level <= 0) return;

        double rad = level / 20D;
        if (BalanceConfig.enable528 && reacher) {
            rad = rad / 49D;
        } else if (reacher) {
            rad = NtmMath.dampedSqrt(rad);
        }

        ContaminationUtil.contaminate(
                target,
                HazardType.RADIATION,
                ContaminationType.CREATIVE,
                rad * RadiationConfig.hazardRate);
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
        double level = HazardModifier.evalAllModifiers(stack, player, base, modifiers);
        if (level < 1e-5D) return;
        appendRadiationLines(list, level, stack.getCount());
    }
}
