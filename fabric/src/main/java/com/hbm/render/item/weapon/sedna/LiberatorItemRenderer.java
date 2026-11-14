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

import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;

public class LiberatorItemRenderer extends AnimatedGunItemRenderer {

    public LiberatorItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.5F * 0.8F, -1.25F * 0.8F, 1.25F * 0.8F, 0, -4.625 / 8D, 0.25)
                        .body()
                        .size(0.375)
                        .mesh(ResourceManager.liberator)
                        .skin(ResourceManager.liberator_tex)
                        .hinge(0, -1, -3, Axis.XP, Rig.DRAW)
                        .hinge(0, -3, -3, Axis.XP, Rig.TILT)
                        .shift(r -> r.x(Rig.KICK) * 2, r -> r.y(Rig.KICK), r -> r.z(Rig.KICK))
                        .spin(Axis.XP, r -> r.z(Rig.KICK) * 10)
                        .part("Gun")
                        .group(
                                barrels ->
                                        barrels.hinge(0, -0.5, 0.75, Axis.XP, Rig.BREAK)
                                                .part("Barrel")
                                                .group(
                                                        shell ->
                                                                shell.move(Rig.SHELL1)
                                                                        .part("Shell1"))
                                                .group(
                                                        shell ->
                                                                shell.move(Rig.SHELL2)
                                                                        .part("Shell2"))
                                                .group(
                                                        shell ->
                                                                shell.move(Rig.SHELL3)
                                                                        .part("Shell3"))
                                                .group(
                                                        shell ->
                                                                shell.move(Rig.SHELL4)
                                                                        .part("Shell4"))
                                                .group(
                                                        latch ->
                                                                latch.hinge(
                                                                                0, 1.15625, 0.75,
                                                                                Axis.XP, Rig.LATCH)
                                                                        .part("Latch")))
                        .group(
                                smoke ->
                                        smoke.shift(0, 0.25, 7.25)
                                                .turn(Axis.YP, 90)
                                                .size(0.375)
                                                .shift(0, 0, 0.25 / 0.375)
                                                .smoke(1)
                                                .shift(0, 0, -0.5 / 0.375)
                                                .smoke(1)
                                                .shift(0, 0.5 / 0.375, 0)
                                                .smoke(1)
                                                .shift(0, 0, 0.5 / 0.375)
                                                .smoke(1))
                        .group(
                                flash ->
                                        flash.shift(0, 0.5, 8)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .size(1.5)
                                                .flash(MuzzleFlash.NORMAL, 75, 5))
                        .build());
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Vec3 KICK = RIG.vec3("RECOIL");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
        static final GunAnimationChannels.Scalar LATCH = RIG.x("LATCH");
        static final GunAnimationChannels.Scalar BREAK = RIG.x("BREAK");
        static final GunAnimationChannels.Vec3 SHELL1 = RIG.vec3("SHELL1");
        static final GunAnimationChannels.Vec3 SHELL2 = RIG.vec3("SHELL2");
        static final GunAnimationChannels.Vec3 SHELL3 = RIG.vec3("SHELL3");
        static final GunAnimationChannels.Vec3 SHELL4 = RIG.vec3("SHELL4");

        static {
            RIG.carries("NULL");
        }
    }
}
