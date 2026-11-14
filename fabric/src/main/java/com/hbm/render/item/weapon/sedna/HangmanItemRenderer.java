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

public class HangmanItemRenderer extends AnimatedGunItemRenderer {

    public HangmanItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.5F * 0.8F, -0.875F * 0.8F, 1.75F * 0.8F, 0, -1.5 / 8D, 1.25)
                        .body()
                        .hinge(1.5F * 0.8F, 0, -1, Axis.YP, Rig.YAW)
                        .spin(Axis.ZP, Rig.ROLL)
                        .move(Rig.SMACK)
                        .size(0.125)
                        .mesh(ResourceManager.hangman)
                        .skin(ResourceManager.hangman_tex)
                        .hinge(0, -4, -10, Axis.XP, Rig.DRAW)
                        .slideZ(Rig.KICK)
                        .part("Rifle")
                        .part("Internals")
                        .group(lid -> lid.hinge(-2.1875, -1.75, 0, Axis.ZP, Rig.LID).part("Lid"))
                        .group(
                                mag ->
                                        mag.move(Rig.CLIP)
                                                .part("Magazine")
                                                .when(
                                                        r -> r.get(Rig.ROUNDS) == 0,
                                                        rounds -> rounds.part("Bullets")))
                        .group(
                                smoke ->
                                        smoke.shift(0, 0, 29)
                                                .turn(Axis.YP, 90)
                                                .size(1.5)
                                                .smoke(0.5))
                        .group(
                                flash ->
                                        flash.shift(0, 0, 29)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .size(2)
                                                .flash(MuzzleFlash.NORMAL, 75, 7.5))
                        .build());
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar ROLL = RIG.z("ROLL");
        static final GunAnimationChannels.Scalar YAW = RIG.y("TURN");
        static final GunAnimationChannels.Vec3 SMACK = RIG.vec3("SMACK");
        static final GunAnimationChannels.Scalar LID = RIG.z("LID");
        static final GunAnimationChannels.Vec3 CLIP = RIG.vec3("MAG");
        static final GunAnimationChannels.Scalar ROUNDS = RIG.x("BULLETS");
    }
}
