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
import net.minecraft.resources.Identifier;

public class HenryItemRenderer extends AnimatedGunItemRenderer {

    public HenryItemRenderer(Identifier texture) {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.25F * 0.8F, -1F * 0.8F, 1.75F * 0.8F, 0, -5 / 8D, 1)
                        .spin(Axis.XP, r -> -2.5F * r.aim())
                        .body()
                        .size(0.375)
                        .mesh(ResourceManager.henry)
                        .skin(texture)
                        .shift(r -> r.x(Rig.KICK) * 2, r -> r.y(Rig.KICK), r -> r.z(Rig.KICK))
                        .spin(Axis.XP, r -> r.z(Rig.KICK) * 5)
                        .spin(Axis.ZP, Rig.ROLL)
                        .move(Rig.THROW)
                        .hinge(0, 1, 0, Axis.ZP, Rig.SPIN)
                        .hinge(0, -4, 4, Axis.XP, Rig.TILT)
                        .hinge(0, 2, -4, Axis.XN, Rig.DRAW)
                        .group(
                                smoke ->
                                        smoke.shift(0, 1, 8)
                                                .spin(Axis.ZN, Rig.ROLL)
                                                .turn(Axis.YP, 90)
                                                .smoke(0.25))
                        .part("Gun")
                        .group(
                                sight ->
                                        sight.hinge(0, 1.25, -0.1875, Axis.XP, Rig.LEAF)
                                                .part("Sight"))
                        .group(
                                hammer ->
                                        hammer.hinge(
                                                        0,
                                                        0.625,
                                                        -3,
                                                        Axis.XP,
                                                        r -> -30 + r.get(Rig.COCK))
                                                .part("Hammer"))
                        .group(
                                lever ->
                                        lever.hinge(0, 0.25, -2.3125, Axis.XP, Rig.CYCLE)
                                                .part("Lever"))
                        .group(front -> front.hinge(0, 1, 0, Axis.ZP, Rig.TWIST).part("Front"))
                        .group(
                                round ->
                                        round.shift(
                                                        r -> r.x(Rig.ROUND),
                                                        r -> r.y(Rig.ROUND),
                                                        r -> r.z(Rig.ROUND) - 1)
                                                .part("Bullet"))
                        .group(
                                flash ->
                                        flash.shift(0, 1, 8)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .flash(MuzzleFlash.NORMAL, 75, 5))
                        .build());
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar LEAF = RIG.x("SIGHT");
        static final GunAnimationChannels.Vec3 KICK = RIG.vec3("RECOIL");
        static final GunAnimationChannels.Scalar COCK = RIG.x("HAMMER");
        static final GunAnimationChannels.Scalar CYCLE = RIG.x("LEVER");
        static final GunAnimationChannels.Scalar ROLL = RIG.z("TURN");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
        static final GunAnimationChannels.Scalar TWIST = RIG.z("TWIST");
        static final GunAnimationChannels.Vec3 ROUND = RIG.vec3("BULLET");
        static final GunAnimationChannels.Vec3 THROW = RIG.vec3("YEET");
        static final GunAnimationChannels.Scalar SPIN = RIG.z("ROLL");
    }
}
