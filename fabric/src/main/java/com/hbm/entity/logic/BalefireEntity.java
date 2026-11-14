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

package com.hbm.entity.logic;

import com.hbm.NuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.entity.logic.ExplosionChunkloadingEntity;
import com.hbm.explosion.NuclearShockwave;
import com.hbm.explosion.spiral.BalefireColumnProcessor;
import com.hbm.explosion.spiral.SpiralExplosion;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BalefireEntity extends ExplosionChunkloadingEntity {

    private static final String CRATER = "exp_";

    public int age = 0;
    public int destructionRange = 0;
    public SpiralExplosion exp;
    public int speed = 1;
    public boolean did = false;

    public BalefireEntity(EntityType<? extends BalefireEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!did) {
            if (!level().isClientSide() && ((RuntimeConfig) HbmConfig.RUNTIME).extendedLogging()) {
                NuclearTech.LOGGER.info(
                        "[NUKE] Initialized BF explosion at {} / {} / {} with strength {}!",
                        getX(),
                        getY(),
                        getZ(),
                        destructionRange);
            }
            exp =
                    new SpiralExplosion(
                            level(),
                            (int) getX(),
                            (int) getY(),
                            (int) getZ(),
                            destructionRange,
                            BalefireColumnProcessor.INSTANCE);
            did = true;
        }
        var finished = false;
        for (var budget = ++speed; budget > 0 && !finished; budget--)
            finished = exp.advanceColumn();
        if (!finished)
            NuclearShockwave.sweep(level(), getX(), getY(), getZ(), destructionRange * 2);
        else discard();
        age++;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        age = input.getIntOr("age", 0);
        destructionRange = input.getIntOr("destructionRange", 0);
        speed = input.getIntOr("speed", 1);
        exp =
                new SpiralExplosion(
                        level(),
                        (int) getX(),
                        (int) getY(),
                        (int) getZ(),
                        destructionRange,
                        BalefireColumnProcessor.INSTANCE);
        exp.read(input, CRATER);
        did = true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("age", age);
        output.putInt("destructionRange", destructionRange);
        output.putInt("speed", speed);
        output.putBoolean("did", did);
        if (exp != null) exp.write(output, CRATER);
    }
}
