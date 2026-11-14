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

package com.hbm.items.food;

import com.hbm.lib.ModDamageTypes;
import com.hbm.potion.HbmPotion;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class FoodAdditives {

    public static final int CYANIDE = 1;
    public static final int RED_PILL = 2;
    private static final RandomSource RANDOM = RandomSource.create();

    private FoodAdditives() {}

    public static boolean isFood(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    public static void apply(LivingEntity entity, int additives) {
        if (!(entity instanceof Player player) || !(entity.level() instanceof ServerLevel level))
            return;
        if ((additives & CYANIDE) != 0) {
            for (int i = 0; i < 10; i++) {
                player.hurtServer(
                        level,
                        level.damageSources()
                                .source(
                                        RANDOM.nextBoolean()
                                                ? ModDamageTypes.EUTHANIZED_SELF
                                                : ModDamageTypes.EUTHANIZED_SELF_2),
                        1_000F);
            }
        }
        if ((additives & RED_PILL) != 0) {
            for (int i = 0; i < 10; i++) {
                player.addEffect(
                        new MobEffectInstance(
                                HbmPotion.death(), 60 * SharedConstants.TICKS_PER_MINUTE, 0));
            }
        }
    }
}
