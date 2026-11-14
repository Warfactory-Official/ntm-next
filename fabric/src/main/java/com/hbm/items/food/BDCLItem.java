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

import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;

public class BDCLItem extends Item {

    public static final int DRINK_TICKS = 40;
    public static final Consumable CONSUMABLE =
            Consumables.defaultDrink().consumeSeconds(DRINK_TICKS / 20F).build();

    private static final int GULP_EVERY = 5;
    private static final int GULP_UNTIL = 10;
    private static final int HURRY_FROM = 24;
    private static final int HURRY_EVERY = 4;

    public BDCLItem(Properties props) {
        super(props);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (remaining % GULP_EVERY == 0 && remaining >= GULP_UNTIL) {
            play(level, entity, ModSounds.PLAYER_GULP.get());
        }

        if (remaining == 1) {
            stack.consume(1, entity);
            entity.stopUsingItem();
            play(level, entity, ModSounds.PLAYER_GROAN.get());
            return;
        }

        if (remaining <= HURRY_FROM && remaining % HURRY_EVERY == 0) entity.useItemRemaining--;
    }

    private static void play(Level level, LivingEntity entity, SoundEvent sound) {
        EntitySounds.play(
                entity,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                sound,
                SoundSource.PLAYERS,
                1F,
                1F);
    }
}
