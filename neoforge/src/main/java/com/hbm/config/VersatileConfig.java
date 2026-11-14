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

package com.hbm.config;

import com.hbm.data.ExplosionData;
import com.hbm.data.ItemData;
import com.hbm.potion.HbmPotion;
import com.hbm.potion.UncurableEffectInstance;
import java.util.Locale;
import net.minecraft.SharedConstants;
import net.minecraft.world.entity.LivingEntity;

public final class VersatileConfig {

    private static final int MINUTE = 60 * 20;
    private static final int HOUR = 60 * MINUTE;

    private VersatileConfig() {}

    public static int getSchrabOreChance() {
        return BalanceConfig.enableLBSM ? ExplosionData.SCHRAB_ORE_RATE.get() : 250;
    }

    public static int getLongDecayChance() {
        return BalanceConfig.enable528 ? 15 * HOUR : shorterDecay() ? 15 * MINUTE : 3 * HOUR;
    }

    public static int getShortDecayChance() {
        return BalanceConfig.enable528 ? 3 * HOUR : shorterDecay() ? 3 * MINUTE : 15 * MINUTE;
    }

    private static boolean shorterDecay() {
        return BalanceConfig.enableLBSM && BalanceConfig.enableLBSMShorterDecay;
    }

    public static void applyPotionSickness(LivingEntity entity, int duration) {
        int mode = potionSicknessMode();
        if (mode == 0) return;
        int seconds = mode == 2 ? duration * 12 : duration;
        entity.addEffect(
                new UncurableEffectInstance(
                        HbmPotion.potionsickness(), seconds * SharedConstants.TICKS_PER_SECOND));
    }

    private static int potionSicknessMode() {
        String s = ItemData.POTION_SICKNESS.get().toLowerCase(Locale.ROOT);
        if ("normal".equals(s)) return 1;
        if ("terraria".equals(s)) return 2;
        return 0;
    }

    public static boolean hasPotionSickness(LivingEntity entity) {
        return entity.hasEffect(HbmPotion.potionsickness());
    }
}
