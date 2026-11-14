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

package com.hbm.explosion;

import com.hbm.config.BombConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.CloudFleijaEntity;
import com.hbm.entity.effect.CloudFleijaRainbowEntity;
import com.hbm.entity.effect.NukeTorexEntity;
import com.hbm.entity.logic.BalefireEntity;
import com.hbm.entity.logic.NukeExplosionMK3Entity;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import com.hbm.sound.WorldSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public final class ExplosionNukeCustom {

    public static final int MAX_TNT = 150;
    public static final int MAX_NUKE = 200;
    public static final int MAX_HYDRO = 350;
    public static final int MAX_AMAT = 350;
    public static final int MAX_SCHRAB = 250;
    public static final int MAX_DIRTY = 100;

    private enum Stage {
        TNT(MAX_TNT),
        NUKE(MAX_NUKE),
        HYDRO(MAX_HYDRO),
        AMAT(MAX_AMAT),
        SCHRAB(MAX_SCHRAB);

        private static final Stage[] BY_RANK = values();

        private final int max;

        Stage(int max) {
            this.max = max;
        }
    }

    private ExplosionNukeCustom() {}

    public static void explodeCustom(
            Level level,
            double x,
            double y,
            double z,
            float tnt,
            float nuke,
            float hydro,
            float amat,
            float dirty,
            float schrab,
            float euph) {
        float fallout = Math.min(dirty, MAX_DIRTY);

        if (euph > 0) {
            var ex = new NukeExplosionMK3Entity(ModEntities.NUKE_EXPLOSION_MK3.get(), level);
            ex.setPos(x, y, z);
            ex.destructionRange = 150;
            ex.speed = BombConfig.blastSpeed;
            ex.coefficient = 1.0F;
            level.addFreshEntity(ex);
            WorldSounds.explosion(
                    level,
                    null,
                    x,
                    y,
                    z,
                    SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.BLOCKS,
                    100000.0F,
                    1.0F);
            level.addFreshEntity(CloudFleijaRainbowEntity.statFac(level, 50, x, y, z));
            return;
        }

        float[] yields = {tnt, nuke, hydro, amat, schrab};
        int rank = yields.length - 1;
        while (rank >= 0 && !(yields[rank] > 0)) rank--;
        if (rank < 0) return;

        float absorbed = 0;
        for (int lower = rank - 1, divisor = 2; lower >= 0; lower--, divisor *= 2)
            absorbed += yields[lower] / divisor;
        var stage = Stage.BY_RANK[rank];
        float yield = Math.min(yields[rank] + absorbed, stage.max);

        double cx = x + 0.5, cy = y + 0.5, cz = z + 0.5, raisedY = y + 5;
        switch (stage) {
            case SCHRAB -> {
                var blast = NukeExplosionMK3Entity.statFacFleija(level, cx, cy, cz, (int) yield);
                if (!blast.isRemoved()) {
                    level.addFreshEntity(blast);
                    level.addFreshEntity(CloudFleijaEntity.statFac(level, (int) yield, cx, cy, cz));
                }
            }
            case AMAT -> {
                var balefire = new BalefireEntity(ModEntities.BALEFIRE.get(), level);
                balefire.setPos(cx, cy, cz);
                balefire.destructionRange = (int) yield;
                level.addFreshEntity(balefire);
                NukeTorexEntity.statFacBale(level, cx, raisedY, cz, yield);
            }
            case HYDRO -> {
                level.addFreshEntity(
                        NukeExplosionMK5Entity.statFac(level, (int) yield, cx, cy, cz)
                                .moreFallout((int) (fallout * 0.25F)));
                NukeTorexEntity.statFac(level, cx, raisedY, cz, yield);
            }
            case NUKE -> {
                level.addFreshEntity(
                        NukeExplosionMK5Entity.statFac(level, (int) yield, cx, raisedY, cz)
                                .moreFallout((int) fallout));
                NukeTorexEntity.statFac(level, cx, raisedY, cz, yield);
            }
            case TNT -> {
                if (yield >= 75) {
                    level.addFreshEntity(
                            NukeExplosionMK5Entity.statFacNoRad(level, (int) yield, cx, cy, cz));
                    NukeTorexEntity.statFac(level, cx, raisedY, cz, yield);
                } else {
                    CompositeExplosion.detonate(
                            level,
                            cx,
                            cy,
                            cz,
                            yield,
                            CompositeExplosion.CLOUD
                                    | CompositeExplosion.RUBBLE
                                    | CompositeExplosion.SHRAPNEL);
                }
            }
        }
    }
}
