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

package com.hbm.entity.grenade;

import com.hbm.NuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.entity.projectile.ProjectileEntity;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class GrenadeBouncyBaseEntity extends ProjectileEntity {
    protected int timer;
    private Vec3 stepStart = Vec3.ZERO;

    protected GrenadeBouncyBaseEntity(
            EntityType<? extends GrenadeBouncyBaseEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean doesImpactEntities() {
        return false;
    }

    @Override
    public void tick() {
        stepStart = position();
        super.tick();
        if (isRemoved()) return;
        ++timer;
        if (timer >= getMaxTimer() && level() instanceof ServerLevel) {
            explode();
            if (((RuntimeConfig) HbmConfig.RUNTIME).extendedLogging()) {
                NuclearTech.LOGGER.info(
                        "[GREN] Set off grenade at {} / {} / {} by {}!",
                        (int) getX(),
                        (int) getY(),
                        (int) getZ(),
                        getOwner() instanceof Player player
                                ? player.getDisplayName().getString()
                                : "null");
            }
        }
    }

    @Override
    protected void onImpact(HitResult result) {
        if (!(result instanceof BlockHitResult hit)) return;
        setPos(stepStart.lerp(hit.getLocation(), 0.6));
        var motion = getDeltaMovement();
        var reflected =
                switch (hit.getDirection().getAxis()) {
                    case X -> motion.multiply(-1, 1, 1);
                    case Y -> motion.multiply(1, -1, 1);
                    case Z -> motion.multiply(1, 1, -1);
                };
        if (motion.length() > 0.05) {
            WorldSounds.play(
                    level(),
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    ModSounds.G_BOUNCE.get(),
                    SoundSource.PLAYERS,
                    2,
                    1);
        }
        setDeltaMovement(reflected.scale(getBounceMod()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Timer", timer);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        timer = input.getIntOr("Timer", 0);
    }

    public abstract void explode();

    protected abstract int getMaxTimer();

    protected abstract double getBounceMod();
}
