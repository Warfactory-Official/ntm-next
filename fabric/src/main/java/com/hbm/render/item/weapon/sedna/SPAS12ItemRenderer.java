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
import com.hbm.particle.casing.Casing;
import com.mojang.math.Axis;

public class SPAS12ItemRenderer extends AnimatedGunItemRenderer {

    public SPAS12ItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.25F * 0.8F, -1.75F * 0.8F, -0.5F * 0.8F, 0, 0, 0)
                        .body()
                        .size(0.5)
                        .turn(Axis.YP, 180)
                        .mesh(ResourceManager.spas_12)
                        .skin(ResourceManager.spas_12_tex)
                        .spin(Axis.XP, Rig.DRAW)
                        .apply(Rig.FRAME)
                        .part("MainBody")
                        .group(pump -> pump.apply(Rig.PUMP).part("PumpGrip"))
                        .group(
                                shell ->
                                        shell.apply(Rig.SHELL)
                                                .skin(ResourceManager.casings_tex)
                                                .batch(
                                                        b ->
                                                                b.part(
                                                                                "Shell",
                                                                                r ->
                                                                                        0xFF000000
                                                                                                | casing(
                                                                                                        r,
                                                                                                        1))
                                                                        .part(
                                                                                "ShellFore",
                                                                                r ->
                                                                                        0xFF000000
                                                                                                | casing(
                                                                                                        r,
                                                                                                        0)))
                                                .group(
                                                        smoke ->
                                                                smoke.shift(0, 1.5, -11)
                                                                        .turn(Axis.YP, -90)
                                                                        .size(0.25)
                                                                        .smoke(0.75))
                                                .group(
                                                        flash ->
                                                                flash.shift(0, 1.5, -11)
                                                                        .turn(Axis.YP, -90)
                                                                        .roll(90)
                                                                        .flash(
                                                                                MuzzleFlash.NORMAL,
                                                                                75,
                                                                                7.5)))
                        .build());
        Rig.RIG.requireParts("spas12", ResourceManager.spas_12_anim);
    }

    private static int casing(GunRenderDefinition.Reading r, int layer) {
        Casing casing = GunRenderDefinition.magazine(r).getCasing(r.stack(), r.inventory());
        if (casing == null) return Casing.BRASS;
        return casing.tint(casing.tintCount() > layer ? layer : 0);
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Part FRAME = RIG.part("MainBody");
        static final GunAnimationChannels.Part PUMP = RIG.part("PumpGrip");
        static final GunAnimationChannels.Part SHELL = RIG.part("Shell");
    }
}
