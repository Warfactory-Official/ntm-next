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

import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;

public class Am180ItemRenderer extends AnimatedGunItemRenderer {

    public Am180ItemRenderer() {
        super(body());
        Rig.RIG.requireParts("am180", ResourceManager.am180_anim);
    }

    private static GunRenderDefinition body() {
        var silenced = GunRenderDefinition.fitted(XWeaponModManager.ID_SILENCER);
        return GunRenderDefinition.on(Rig.RIG)
                .shift(0, 0, 0.875)
                .aim(-1F * 0.8F, -1F * 0.8F, 0.8F, 0, -4.1875 / 8D, 0.25)
                .body()
                .size(0.1875)
                .mesh(ResourceManager.am180)
                .skin(ResourceManager.am180_tex)
                .hinge(0, -2, -6, Axis.XP, Rig.DRAW)
                .spin(Axis.ZP, Rig.ROLL)
                .slideZ(Rig.KICK)
                .apply(Rig.GUN)
                .part("Gun")
                .when(silenced, s -> s.part("Silencer"))
                .group(trigger -> trigger.apply(Rig.TRIGGER).part("Trigger"))
                .group(bolt -> bolt.slideZ(Rig.CYCLE).apply(Rig.BOLT).part("Bolt"))
                .group(
                        pan ->
                                pan.move(Rig.PAN_SHIFT)
                                        .about(
                                                0,
                                                2.0625,
                                                3.75,
                                                a ->
                                                        a.spin(Axis.XP, r -> r.x(Rig.PAN_TURN))
                                                                .spin(
                                                                        Axis.ZP,
                                                                        r -> r.z(Rig.PAN_TURN)))
                                        .hinge(0, 2.3125, 1.5, Axis.XP, Rig.PAN_FLIP)
                                        .apply(Rig.PAN)
                                        .group(
                                                disc ->
                                                        disc.hinge(
                                                                        0,
                                                                        0,
                                                                        1.5,
                                                                        Axis.YN,
                                                                        r ->
                                                                                GunRenderDefinition
                                                                                                .magazine(
                                                                                                        r)
                                                                                                .getAmount(
                                                                                                        r
                                                                                                                .stack(),
                                                                                                        r
                                                                                                                .inventory())
                                                                                        / 59D
                                                                                        * 360D)
                                                                .part("Mag"))
                                        .part("MagPlate"))
                .group(
                        smoke ->
                                smoke.either(
                                                silenced,
                                                end -> end.shift(0, 1.875, 17),
                                                end -> end.shift(0, 1.875, 13))
                                        .spin(Axis.ZN, Rig.ROLL)
                                        .turn(Axis.YP, 90)
                                        .smoke(0.25))
                .group(
                        flash ->
                                flash.either(
                                        silenced,
                                        can ->
                                                can.shift(0, 1.875, 16.75)
                                                        .turn(Axis.YP, 90)
                                                        .roll(90)
                                                        .size(0.5)
                                                        .flash(MuzzleFlash.NORMAL, 75, 5),
                                        open ->
                                                open.shift(0, 1.875, 12)
                                                        .turn(Axis.YP, 90)
                                                        .roll(90)
                                                        .size(0.75)
                                                        .flash(MuzzleFlash.NORMAL, 50, 7.5)))
                .build();
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Vec3 PAN_SHIFT = RIG.vec3("MAG");
        static final GunAnimationChannels.Vec3 PAN_TURN = RIG.vec3("MAGTURN");
        static final GunAnimationChannels.Scalar PAN_FLIP = RIG.x("MAGSPIN");
        static final GunAnimationChannels.Scalar CYCLE = RIG.z("BOLT");
        static final GunAnimationChannels.Scalar ROLL = RIG.z("TURN");
        static final GunAnimationChannels.Part GUN = RIG.part("Gun");
        static final GunAnimationChannels.Part TRIGGER = RIG.part("Trigger");
        static final GunAnimationChannels.Part BOLT = RIG.part("Bolt");
        static final GunAnimationChannels.Part PAN = RIG.part("Mag");
    }
}
