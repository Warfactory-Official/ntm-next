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
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;

public class DoubleBarrelItemRenderer extends AnimatedGunItemRenderer {

    public DoubleBarrelItemRenderer(Identifier texture) {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.25F * 0.8F, -1F * 0.8F, 2F * 0.8F, 0, -2 / 8D, 1)
                        .body()
                        .size(0.375)
                        .mesh(ResourceManager.double_barrel)
                        .skin(texture)
                        .shift(r -> r.x(Rig.KICK) * 3, r -> r.y(Rig.KICK), r -> r.z(Rig.KICK))
                        .spin(Axis.XP, r -> r.z(Rig.KICK) * 10)
                        .hinge(0, 0, -4, Axis.XN, Rig.DRAW)
                        .hinge(0, 0, -4, Axis.YP, Rig.YAW)
                        .hinge(0, 0, -4, Axis.XN, Rig.TILT)
                        .part("Stock")
                        .group(
                                barrels ->
                                        barrels.hinge(0, -0.4375, -0.875, Axis.XP, Rig.BREAK)
                                                .part("BarrelShort")
                                                .when(
                                                        r ->
                                                                !(r.stack().getItem()
                                                                                == ModItems
                                                                                        .GUN_DOUBLE_BARREL_SACRED_DRAGON
                                                                                        .get()
                                                                        || XWeaponModManager
                                                                                .hasUpgrade(
                                                                                        r.stack(),
                                                                                        0,
                                                                                        XWeaponModManager
                                                                                                .ID_SAWED_OFF)),
                                                        full -> full.part("Barrel"))
                                                .group(
                                                        buckle ->
                                                                buckle.hinge(
                                                                                0.75,
                                                                                0,
                                                                                -0.6875,
                                                                                Axis.YP,
                                                                                Rig.BUCKLE)
                                                                        .part("Buckle"))
                                                .group(
                                                        lever ->
                                                                lever.hinge(
                                                                                -0.3125, 0.3125, 0,
                                                                                Axis.ZP, Rig.LEVER)
                                                                        .part("Lever"))
                                                .when(
                                                        r -> r.get(Rig.EMPTY) == 0,
                                                        loaded ->
                                                                loaded.group(
                                                                        shells ->
                                                                                shells.move(
                                                                                                Rig
                                                                                                        .SHELLS)
                                                                                        .hinge(
                                                                                                0,
                                                                                                0,
                                                                                                -1,
                                                                                                Axis
                                                                                                        .XP,
                                                                                                Rig
                                                                                                        .SHELL_FLIP)
                                                                                        .part(
                                                                                                "Shells"))))
                        .group(
                                flash ->
                                        flash.shift(0, 0, 8)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .size(2)
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
        static final GunAnimationChannels.Vec3 KICK = RIG.vec3("RECOIL");
        static final GunAnimationChannels.Scalar YAW = RIG.y("TURN");
        static final GunAnimationChannels.Scalar BREAK = RIG.x("BARREL");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
        static final GunAnimationChannels.Vec3 SHELLS = RIG.vec3("SHELLS");
        static final GunAnimationChannels.Scalar SHELL_FLIP = RIG.x("SHELL_FLIP");
        static final GunAnimationChannels.Scalar LEVER = RIG.z("LEVER");
        static final GunAnimationChannels.Scalar BUCKLE = RIG.y("BUCKLE");
        static final GunAnimationChannels.Scalar EMPTY = RIG.x("NO_AMMO");
    }
}
