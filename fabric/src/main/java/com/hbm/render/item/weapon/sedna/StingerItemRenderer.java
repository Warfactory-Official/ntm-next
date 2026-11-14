/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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
import net.minecraft.world.item.ItemStack;

public class StingerItemRenderer extends AnimatedGunItemRenderer {
    private static final String LABEL = "Not accurate";
    private static final int LABEL_COLOR = 0xFFFF0000;

    public StingerItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-3.75F * 0.8F, -9F * 0.8F, -3.5F * 0.8F, -2.625F * 0.8F, -6.5, -8.5F)
                        .body()
                        .when(
                                r -> !r.steadyAim(),
                                gun ->
                                        gun.size(1.5)
                                                .hinge(0, -1, -1, Axis.XP, Rig.DRAW)
                                                .hinge(0, -4, -3, Axis.XP, Rig.LOAD)
                                                .group(
                                                        tube ->
                                                                tube.turn(Axis.YP, 180)
                                                                        .mesh(
                                                                                ResourceManager
                                                                                        .stinger)
                                                                        .skin(
                                                                                ResourceManager
                                                                                        .stinger_tex)
                                                                        .whole())
                                                .group(
                                                        rocket ->
                                                                rocket.shift(
                                                                                r ->
                                                                                        r.x(
                                                                                                Rig
                                                                                                        .ROCKET),
                                                                                r ->
                                                                                        r.y(
                                                                                                        Rig
                                                                                                                .ROCKET)
                                                                                                + 3.5,
                                                                                r ->
                                                                                        r.z(
                                                                                                        Rig
                                                                                                                .ROCKET)
                                                                                                - 3)
                                                                        .mesh(
                                                                                ResourceManager
                                                                                        .panzerschreck)
                                                                        .skin(
                                                                                ResourceManager
                                                                                        .panzerschreck_tex)
                                                                        .part("Rocket")
                                                                        .label(
                                                                                _ ->
                                                                                        LABEL,
                                                                                _ ->
                                                                                        LABEL_COLOR,
                                                                                0.025,
                                                                                -0.5,
                                                                                -3,
                                                                                Axis.ZP,
                                                                                Axis.YP
                                                                                        .rotationDegrees(
                                                                                                90),
                                                                                Axis.XN
                                                                                        .rotationDegrees(
                                                                                                45)))
                                                .group(
                                                        flash ->
                                                                flash.shift(0, 0, 6.5)
                                                                        .turn(Axis.YP, 90)
                                                                        .roll(90)
                                                                        .size(0.75)
                                                                        .flash(
                                                                                MuzzleFlash.NORMAL,
                                                                                150,
                                                                                7.5)))
                        .build());
    }

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar LOAD = RIG.x("RELOAD");
        static final GunAnimationChannels.Vec3 ROCKET = RIG.vec3("ROCKET");
    }
}
