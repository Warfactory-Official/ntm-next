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

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AppleSchrabidiumItem extends AppleBaseItem {
    public static final int FOREVER = Integer.MAX_VALUE;

    public AppleSchrabidiumItem(Properties props, Tier tier) {
        super(props, tier);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide()) return rest;

        if (tier == Tier.NUGGET) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 4));
            entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
        } else if (tier == Tier.INGOT) {
            grant(entity, 1200, 4, 4, 0, 4, 2, 2, 4, 9, 4, 9);
        } else {
            grant(entity, FOREVER, 4, 1, 0, 9, 4, 3, 4, 24, 14, 99);
        }
        return rest;
    }

    private static void grant(
            LivingEntity entity,
            int ticks,
            int regen,
            int resist,
            int fire,
            int damage,
            int dig,
            int speed,
            int jump,
            int health,
            int absorb,
            int saturation) {
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, regen));
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, resist));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, fire));
        entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, damage));
        entity.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, dig));
        entity.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, speed));
        entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks, jump));
        entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, ticks, health));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, absorb));
        entity.addEffect(new MobEffectInstance(MobEffects.SATURATION, ticks, saturation));
    }
}
