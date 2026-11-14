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

public class LagItemRenderer extends AnimatedGunItemRenderer {

    public LagItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.5F * 0.8F, -1F * 0.8F, 1.5F * 0.8F, 0, -3.375 / 8D, 0.5)
                        .body()
                        .size(0.25)
                        .turn(Axis.YP, 90)
                        .mesh(ResourceManager.mike_hawk)
                        .skin(ResourceManager.mike_hawk_tex)
                        .hinge(4, -4, 0, Axis.ZN, Rig.DRAW)
                        .move(Rig.SHIFT)
                        .spin(Axis.ZP, r -> r.z(Rig.TURN))
                        .spin(Axis.YP, r -> r.y(Rig.TURN))
                        .group(
                                gun ->
                                        gun.apply(Rig.GRIP)
                                                .part("Grip")
                                                .group(
                                                        slide ->
                                                                slide.apply(Rig.SLIDE)
                                                                        .part("Slide"))
                                                .group(
                                                        hammer ->
                                                                hammer.about(
                                                                                3.125,
                                                                                0.125,
                                                                                0,
                                                                                a ->
                                                                                        a.turn(
                                                                                                Axis
                                                                                                        .ZN,
                                                                                                25))
                                                                        .apply(Rig.HAMMER)
                                                                        .part("Hammer"))
                                                .when(
                                                        r ->
                                                                GunRenderDefinition.magazine(r)
                                                                                .getAmount(
                                                                                        r.stack(),
                                                                                        null)
                                                                        > 0,
                                                        loaded ->
                                                                loaded.group(
                                                                        round ->
                                                                                round.apply(
                                                                                                Rig
                                                                                                        .ROUND)
                                                                                        .part(
                                                                                                "Bullet")))
                                                .group(
                                                        mag ->
                                                                mag.apply(Rig.MAGAZINE)
                                                                        .part("Magazine"))
                                                .group(
                                                        smoke ->
                                                                smoke.shift(-10.25, 1, 0)
                                                                        .size(0.5)
                                                                        .smoke(0.5))
                                                .group(
                                                        flash ->
                                                                flash.shift(-10.25, 1, 0)
                                                                        .roll(90)
                                                                        .flash(
                                                                                MuzzleFlash.NORMAL,
                                                                                75,
                                                                                7.5)))
                        .build());
        Rig.RIG.requireParts("lag", ResourceManager.lag_anim);
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Vec3 SHIFT = RIG.vec3("ADD_TRANS");
        static final GunAnimationChannels.Vec3 TURN = RIG.vec3("ADD_ROT");
        static final GunAnimationChannels.Part GRIP = RIG.part("Grip");
        static final GunAnimationChannels.Part SLIDE = RIG.part("Slide");
        static final GunAnimationChannels.Part HAMMER = RIG.part("Hammer");
        static final GunAnimationChannels.Part ROUND = RIG.part("Bullet");
        static final GunAnimationChannels.Part MAGAZINE = RIG.part("Magazine");
    }
}
