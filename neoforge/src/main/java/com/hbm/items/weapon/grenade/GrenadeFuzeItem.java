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

package com.hbm.items.weapon.grenade;

import com.hbm.entity.grenade.GrenadeUniversalEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GrenadeFuzeItem extends Item {

    public final EnumGrenadeFuze type;

    public GrenadeFuzeItem(Item.Properties properties, EnumGrenadeFuze type) {
        super(properties);
        this.type = type;
    }

    public static void updateTick(GrenadeUniversalEntity grenade) {
        switch (grenade.getFuze()) {
            case S3 -> {
                if (grenade.getTimer() >= 60) grenade.explode();
            }
            case S7 -> {
                if (grenade.getTimer() >= 140) grenade.explode();
            }
            case S15 -> {
                if (grenade.getTimer() >= 300) grenade.explode();
            }
            case AIRBURST -> airburst(grenade);
            case IMPACT -> {}
        }
    }

    public static void onImpact(GrenadeUniversalEntity grenade, HitResult impact) {
        switch (grenade.getFuze()) {
            case IMPACT -> {
                if (grenade.getTimer() >= 10) {
                    Vec3 hit = impact.getLocation();
                    grenade.setPos(hit.x, hit.y, hit.z);
                    grenade.explode();
                }
            }
            case S3, S7, S15, AIRBURST -> {}
        }
    }

    private static void airburst(GrenadeUniversalEntity grenade) {
        if (grenade.getTimer() < 30) return;
        HitResult trace =
                grenade.level()
                        .clip(
                                new ClipContext(
                                        grenade.position(),
                                        grenade.position().add(0D, -10D, 0D),
                                        ClipContext.Block.OUTLINE,
                                        ClipContext.Fluid.NONE,
                                        grenade));
        if (trace.getType() == HitResult.Type.BLOCK) grenade.explode();
    }

    public enum EnumGrenadeFuze {
        S3(0x000000),
        S7(0x404040),
        S15(0x808080),
        IMPACT(0xE36C17),
        AIRBURST(0x56A137);

        public static final EnumGrenadeFuze[] VALUES = values();

        private final int bandColor;

        EnumGrenadeFuze(int bandColor) {
            this.bandColor = bandColor;
        }

        public int bandColor() {
            return bandColor;
        }
    }
}
