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

package com.hbm.items.tool;

import com.hbm.extprop.LivingHazards;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class DosimeterItem extends Item {

    private static final ClickSoundRanges CLICKS =
            new ClickSoundRanges()
                    .band(0, rate -> rate < 0.5)
                    .band(1, rate -> rate < 1)
                    .band(2, rate -> rate >= 0.5 && rate < 2)
                    .band(3, rate -> rate >= 2);

    private final RandomSource random = RandomSource.createThreadSafe();

    public DosimeterItem(Properties props) {
        super(props);
    }

    private static int pickGeiger(double rate, RandomSource random) {
        return CLICKS.pick(rate, random);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof LivingEntity entity) || !TickPhase.every(entity, 5)) return;

        double rate = LivingHazards.fieldLastSecond(entity);
        int click = rate > 1e-5 ? pickGeiger(rate, random) : random.nextInt(100) == 0 ? 1 : 0;
        if (click > 0) {
            EntitySounds.play(
                    entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.GEIGER[click - 1].get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            ContaminationUtil.printDosimeterData(player);
        }
        return InteractionResult.SUCCESS;
    }
}
