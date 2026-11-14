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

public class PepperboxItemRenderer extends AnimatedGunItemRenderer {

    public PepperboxItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 1.5)
                        .aim(-1.25F * 0.8F, -0.75F * 0.8F, 0.8F, 0, -2.5 / 8D, 0.5)
                        .body()
                        .size(0.25)
                        .mesh(ResourceManager.pepperbox)
                        .skin(ResourceManager.pepperbox_tex)
                        .move(Rig.SHIFT)
                        .hinge(0, 0, -5, Axis.XN, Rig.KICK)
                        .group(smoke -> smoke.shift(0, 0.5, 7).turn(Axis.YP, 90).smoke(0.5))
                        .when(
                                r ->
                                        r.x(Rig.LOADER) != 0
                                                || r.y(Rig.LOADER) != 0
                                                || r.z(Rig.LOADER) != 0,
                                loading ->
                                        loading.group(
                                                loader ->
                                                        loader.move(Rig.LOADER)
                                                                .part("Speedloader")
                                                                .when(
                                                                        r -> r.get(Rig.SHOT) != 0,
                                                                        shot -> shot.part("Shot"))))
                        .part("Grip")
                        .group(cylinder -> cylinder.spin(Axis.ZP, Rig.CYLINDER).part("Cylinder"))
                        .group(
                                hammer ->
                                        hammer.hinge(0, 0.375, -1.875, Axis.XP, Rig.COCK)
                                                .part("Hammer"))
                        .group(
                                trigger ->
                                        trigger.slideZ(r -> -r.get(Rig.TRIGGER) * 0.5)
                                                .part("Trigger"))
                        .group(
                                flash ->
                                        flash.shift(0, 0.5, 7)
                                                .size(0.5)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .flash(MuzzleFlash.NORMAL, 75, 15)
                                                .turn(Axis.XP, 45)
                                                .flash(MuzzleFlash.NORMAL, 75, 15))
                        .build());
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar KICK = RIG.x("RECOIL");
        static final GunAnimationChannels.Scalar CYLINDER = RIG.x("ROTATE");
        static final GunAnimationChannels.Scalar COCK = RIG.x("HAMMER");
        static final GunAnimationChannels.Scalar TRIGGER = RIG.x("TRIGGER");
        static final GunAnimationChannels.Vec3 SHIFT = RIG.vec3("TRANSLATE");
        static final GunAnimationChannels.Vec3 LOADER = RIG.vec3("LOADER");
        static final GunAnimationChannels.Scalar SHOT = RIG.x("SHOT");
    }
}
