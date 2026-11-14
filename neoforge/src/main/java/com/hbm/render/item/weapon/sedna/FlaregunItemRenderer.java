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

public class FlaregunItemRenderer extends AnimatedGunItemRenderer {

    public FlaregunItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.25F * 0.8F, -1.5F * 0.8F, 2F * 0.8F, 0, -5.5 / 8D, 0.5)
                        .body()
                        .size(0.125)
                        .mesh(ResourceManager.flaregun)
                        .skin(ResourceManager.flaregun_tex)
                        .move(Rig.KICK)
                        .spin(Axis.XP, r -> r.z(Rig.KICK) * 10)
                        .spin(Axis.XP, Rig.FLIP)
                        .hinge(0, 0, -8, Axis.XN, Rig.DRAW)
                        .part("Gun")
                        .group(
                                hammer ->
                                        hammer.hinge(
                                                        0,
                                                        1.8125,
                                                        -4,
                                                        Axis.XP,
                                                        r -> r.get(Rig.COCK) - 15)
                                                .part("Hammer"))
                        .group(
                                barrel ->
                                        barrel.hinge(0, 2.156, 1.78, Axis.XP, Rig.BREAK)
                                                .part("Barrel")
                                                .move(Rig.SHELL)
                                                .part("Flare"))
                        .group(
                                smoke ->
                                        smoke.shift(0, 4, 9)
                                                .turn(Axis.YP, 90)
                                                .size(0.5)
                                                .smoke(2.5)
                                                .shift(0, 0, 0.1)
                                                .smoke(2))
                        .build());
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Vec3 KICK = RIG.vec3("RECOIL");
        static final GunAnimationChannels.Scalar COCK = RIG.x("HAMMER");
        static final GunAnimationChannels.Scalar BREAK = RIG.x("OPEN");
        static final GunAnimationChannels.Vec3 SHELL = RIG.vec3("SHELL");
        static final GunAnimationChannels.Scalar FLIP = RIG.x("FLIP");
    }
}
