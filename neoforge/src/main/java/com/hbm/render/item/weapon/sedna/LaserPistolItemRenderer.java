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

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;

public class LaserPistolItemRenderer extends AnimatedGunItemRenderer {

    public LaserPistolItemRenderer(Identifier texture) {
        super(body(texture));
    }

    private static GunRenderDefinition body(Identifier texture) {
        Predicate<GunRenderDefinition.Reading> pewPew =
                r -> r.stack().getItem() == ModItems.GUN_LASER_PISTOL_PEW_PEW.get();
        return GunRenderDefinition.on(Rig.RIG)
                .shift(0, 0, 0.875)
                .aim(-1.75F * 0.8F, -2F * 0.8F, 2.75F * 0.8F, 0, -10 / 8D, 1.25)
                .body()
                .size(0.375)
                .mesh(ResourceManager.laser_pistol)
                .skin(texture)
                .hinge(0, -1, -6, Axis.XP, Rig.DRAW)
                .hinge(0, 2, -2, Axis.XP, Rig.TILT)
                .hinge(0, -1, -1, Axis.XP, Rig.SWIRL)
                .slideZ(Rig.KICK)
                .move(Rig.JOLT)
                .part("Gun")
                .when(pewPew, extra -> extra.part("Capacitors"))
                .when(pewPew, extra -> extra.part("Tape"))
                .group(
                        latch ->
                                latch.hinge(1.125, 0, -1.9125, Axis.YP, Rig.LATCH)
                                        .part("Latch")
                                        .move(Rig.CELL)
                                        .part("Battery"))
                .group(
                        flash ->
                                flash.shift(0, 2, 4.75)
                                        .turn(Axis.YP, 90)
                                        .either(
                                                r ->
                                                        r.stack().getItem()
                                                                == ModItems
                                                                        .GUN_LASER_PISTOL_MORNING_GLORY
                                                                        .get(),
                                                emerald -> beam(emerald, 0x008000, 0x80ff00),
                                                ruby -> beam(ruby, 0xff0000, 0xff8000)))
                .build();
    }

    private static void beam(GunRenderDefinition.Builder b, int outer, int core) {
        b.flash(MuzzleFlash.LASER, 150, 1.5, outer)
                .shift(0, 0, -0.25)
                .flash(MuzzleFlash.LASER, 150, 0.75, core);
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar LATCH = RIG.y("LATCH");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
        static final GunAnimationChannels.Vec3 JOLT = RIG.vec3("JOLT");
        static final GunAnimationChannels.Vec3 CELL = RIG.vec3("BATTERY");
        static final GunAnimationChannels.Scalar SWIRL = RIG.x("SWIRL");
    }
}
