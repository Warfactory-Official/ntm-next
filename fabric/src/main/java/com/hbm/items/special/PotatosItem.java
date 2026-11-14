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

package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.BatteryItem;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class PotatosItem extends BatteryItem {
    private static final int BASE_DELAY = 200, JITTER = 100;

    public PotatosItem(Properties properties, long maxCharge, long chargeRate, long dischargeRate) {
        super(properties, maxCharge, chargeRate, dischargeRate);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (getCharge(stack) == 0) return;

        int timer = stack.getOrDefault(ModDataComponents.POTATO_TIMER.get(), 0);
        if (timer > 0) {
            stack.set(ModDataComponents.POTATO_TIMER.get(), timer - 1);
            return;
        }
        if (!(owner instanceof Player player) || player.getMainHandItem() != stack) return;

        float pitch = (float) getCharge(stack) / (float) getMaxCharge(stack) * 0.5F + 0.5F;
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.POTATOS_RANDOM.get(),
                SoundSource.PLAYERS,
                1.0F,
                pitch);
        stack.set(
                ModDataComponents.POTATO_TIMER.get(),
                BASE_DELAY + level.getRandom().nextInt(JITTER));
    }
}
