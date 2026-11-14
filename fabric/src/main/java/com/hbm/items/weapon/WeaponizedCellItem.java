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

package com.hbm.items.weapon;

import com.hbm.data.ItemData;
import com.hbm.entity.effect.CloudFleijaRainbowEntity;
import com.hbm.entity.logic.NukeExplosionMK3Entity;
import com.hbm.interfaces.DroppedItemUpdateHandler;
import com.hbm.sound.WorldSounds;
import java.util.function.Consumer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class WeaponizedCellItem extends Item implements DroppedItemUpdateHandler {

    public static final int FUSE = 50 * 20;

    public WeaponizedCellItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();

        if (entity.tickCount > FUSE || entity.isOnFire()) {
            if (ItemData.DROP_STAR.get()) {
                NukeExplosionMK3Entity blast =
                        NukeExplosionMK3Entity.statFacFleija(
                                level, entity.getX(), entity.getY(), entity.getZ(), 100);
                if (!blast.isRemoved()) {
                    WorldSounds.play(
                            level,
                            null,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            SoundEvents.GENERIC_EXPLODE.value(),
                            SoundSource.BLOCKS,
                            100.0F,
                            level.getRandom().nextFloat() * 0.1F + 0.9F);
                    level.addFreshEntity(blast);
                    level.addFreshEntity(
                            CloudFleijaRainbowEntity.statFac(
                                    level, 100, entity.getX(), entity.getY(), entity.getZ()));
                }
            }
            entity.discard();
            return true;
        }

        int remaining = Math.max(FUSE - entity.tickCount, 1);
        spark(
                level,
                entity,
                level.getRandom().nextInt(FUSE) >= remaining
                        ? DustParticleOptions.REDSTONE
                        : ParticleTypes.SMOKE);
        if (remaining < 100) spark(level, entity, ParticleTypes.LAVA);
        return false;
    }

    private static void spark(ServerLevel level, ItemEntity entity, ParticleOptions particle) {
        level.sendParticles(
                particle,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                1,
                entity.getBbWidth() / 2,
                entity.getBbHeight(),
                entity.getBbWidth() / 2,
                0.0D);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.weaponizedCell.aChargedEnergy"));
        adder.accept(Component.translatable("desc.item.weaponizedCell.whenLeftOn"));
    }
}
