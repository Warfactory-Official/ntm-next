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

public class M2ItemRenderer extends AnimatedGunItemRenderer {

    public M2ItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.5F * 0.8F, -2.5F * 0.8F, 1.75F * 0.8F, 0, -12.5 / 8D, 1.75)
                        .body()
                        .size(0.75)
                        .hinge(0, 1, -2.25, Axis.XP, Rig.DRAW)
                        .slideZ(Rig.KICK)
                        .group(
                                gun ->
                                        gun.turn(Axis.YP, 180)
                                                .mesh(ResourceManager.m2)
                                                .skin(ResourceManager.m2_tex)
                                                .whole())
                        .group(
                                smoke ->
                                        smoke.shift(0, 1.625, 5)
                                                .turn(Axis.YP, 90)
                                                .size(0.5)
                                                .smoke(0.375))
                        .group(
                                flash ->
                                        flash.shift(0, 1.625, 5)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .size(0.5)
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
    }
}
