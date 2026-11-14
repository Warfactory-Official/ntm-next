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

import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.main.ResourceManager;
import com.hbm.particle.casing.Casing;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.mojang.math.Axis;

public class CongoLakeItemRenderer extends AnimatedGunItemRenderer {

    public CongoLakeItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.5F * 0.8F, -2F * 0.8F, 1.25F * 0.8F, 0, -10 / 8D, 0.25)
                        .body()
                        .size(0.5)
                        .mesh(ResourceManager.congolake)
                        .skin(ResourceManager.congolake_tex)
                        .apply(Rig.GUN)
                        .part("Gun")
                        .group(pump -> pump.apply(Rig.PUMP).part("Pump"))
                        .group(
                                sight ->
                                        sight.apply(Rig.SIGHT)
                                                .hinge(0, 2.125, 3, Axis.XP, r -> r.aim() * -90)
                                                .part("Sight"))
                        .group(loop -> loop.apply(Rig.LOOP).part("Loop"))
                        .group(
                                guard ->
                                        guard.apply(Rig.GUARD_OUTER)
                                                .part("GuardOuter")
                                                .group(
                                                        inner ->
                                                                inner.apply(Rig.GUARD_INNER)
                                                                        .part("GuardInner")))
                        .group(
                                shell ->
                                        shell.when(
                                                CongoLakeItemRenderer::shellShown,
                                                shown ->
                                                        shown.apply(Rig.SHELL)
                                                                .skin(ResourceManager.casings_tex)
                                                                .part(
                                                                        "Shell",
                                                                        r ->
                                                                                0xFF000000
                                                                                        | (casing(
                                                                                                        r,
                                                                                                        0)
                                                                                                & 0xFFFFFF))
                                                                .part(
                                                                        "ShellFore",
                                                                        r ->
                                                                                0xFF000000
                                                                                        | (casing(
                                                                                                        r,
                                                                                                        1)
                                                                                                & 0xFFFFFF))))
                        .group(
                                smoke ->
                                        smoke.shift(0, 1.75, 4.25)
                                                .spin(Axis.ZN, r -> r.rotation(Rig.GUN, 2))
                                                .spin(Axis.YN, r -> r.rotation(Rig.GUN, 1))
                                                .spin(Axis.XN, r -> r.rotation(Rig.GUN, 0))
                                                .turn(Axis.YP, 90)
                                                .size(0.25)
                                                .smoke(1))
                        .group(
                                flash ->
                                        flash.shift(0, 1.75, 4.25)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .size(0.5)
                                                .flash(MuzzleFlash.NORMAL, 150, 7.5))
                        .build());
        Rig.RIG.requireParts("congolake", ResourceManager.congolake_anim);
    }

    private static boolean shellShown(GunRenderDefinition.Reading r) {
        return GunBaseNTItem.getLastAnim(r.stack(), 0) != GunAnimation.INSPECT
                || GunRenderDefinition.magazine(r).getAmount(r.stack(), r.inventory()) > 0;
    }

    private static int casing(GunRenderDefinition.Reading r, int layer) {
        Casing casing = GunRenderDefinition.magazine(r).getCasing(r.stack(), r.inventory());
        if (casing == null) return Casing.GRENADE_40MM;
        return casing.tint(casing.tintCount() > layer ? layer : 0);
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Part GUN = RIG.part("Gun");
        static final GunAnimationChannels.Part PUMP = RIG.part("Pump");
        static final GunAnimationChannels.Part SIGHT = RIG.part("Sight");
        static final GunAnimationChannels.Part LOOP = RIG.part("Loop");
        static final GunAnimationChannels.Part GUARD_OUTER = RIG.part("GuardOuter");
        static final GunAnimationChannels.Part GUARD_INNER = RIG.part("GuardInner");
        static final GunAnimationChannels.Part SHELL = RIG.part("Shell");
    }
}
