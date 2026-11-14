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
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class TurretHowardDamagedBlockEntity extends TurretHowardBlockEntity {

    private static final int BURST_PERIOD = 4;

    public TurretHowardDamagedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_HOWARD_DAMAGED.get(), pos, state);
    }

    @Override
    public boolean hasPower() {
        return true;
    }

    @Override
    public boolean isOn() {
        return true;
    }

    @Override
    public long getConsumption() {
        return 0L;
    }

    @Override
    protected boolean hasConnectorPlugs() {
        return false;
    }

    @Override
    public boolean entityAcceptableTarget(Entity e) {
        return e instanceof LivingEntity
                && !(e instanceof Player player && player.getAbilities().instabuild);
    }

    @Override
    public void updateFiringTick() {
        if (!beat(BURST_PERIOD) || tPos == null) return;
        var random = level.getRandom();
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.TURRET_HOWARD_FIRE.get(),
                SoundSource.BLOCKS,
                4.0F,
                0.7F + random.nextFloat() * 0.3F);

        cachedCasingConfig = DGK_CASING;
        spawnCasing();

        int roll = random.nextInt(100) + 1;
        int accuracy = ((RuntimeConfig) HbmConfig.RUNTIME).ciwsAccuracy();
        if (roll <= 0.5 * accuracy && target != null) {
            var shrapnel = level.damageSources().source(ModDamageTypes.SHRAPNEL);
            target.hurtServer(
                    (ServerLevel) level,
                    ModifiedDamageSource.bypassingCooldown(shrapnel),
                    2F + random.nextInt(2));
        }

        muzzleFlash(barrelTip().add(alongBarrel(0, 0.25, 0)), 1.5F, 1);
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
    public double getDetectorRange() {
        return 16D;
    }

    @Override
    public double getDetectorGrace() {
        return 5D;
    }

    @Override
    public boolean hasThermalVision() {
        return false;
    }
}
