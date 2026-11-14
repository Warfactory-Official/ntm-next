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

package com.hbm.blockentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory9mm;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TurretSentryDamagedBlockEntity extends TurretSentryBlockEntity {

    public TurretSentryDamagedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_SENTRY_DAMAGED.get(), pos, state);
    }

    @Override
    public boolean hasPower() {
        return true;
    }

    @Override
    public long getConsumption() {
        return 0L;
    }

    @Override
    public boolean isOn() {
        return true;
    }

    @Override
    public double getTurretYawSpeed() {
        return 3D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 2D;
    }

    @Override
    public boolean hasThermalVision() {
        return false;
    }

    @Override
    public boolean entityAcceptableTarget(Entity e) {
        if (e instanceof Player player && player.getAbilities().instabuild) return false;
        return e instanceof LivingEntity;
    }

    @Override
    public void updateFiringTick() {
        if (beat(10)) {
            BulletConfig conf = XFactory9mm.p9_fmj;
            Vec3 flash = getTurretPos();
            cachedCasingConfig = conf.casing;

            if (shotSide) {
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition,
                        ModSounds.TURRET_SENTRY_FIRE.get(),
                        SoundSource.BLOCKS,
                        2.0F,
                        1.0F);
                spawnBullet(conf, 5F);
                Vec3 side = new Vec3(0.125, 0, 0).yRot((float) -rotationYaw);
                flash = barrelTip().add(side.x, 0, side.z);
            } else {
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition,
                        ModSounds.TURRET_SENTRY_FIRE.get(),
                        SoundSource.BLOCKS,
                        2.0F,
                        0.75F);
                if (casingDelay() == 0) spawnCasing();
                else casingDelay = casingDelay();
            }

            muzzleFlash(flash, 1F, 1);

            if (shotSide) hbm$syncSet$TurretSentryBlockEntity$didJustShootLeft(true);
            else hbm$syncSet$TurretSentryBlockEntity$didJustShootRight(true);
            shotSide = !shotSide;
        }
    }
}
