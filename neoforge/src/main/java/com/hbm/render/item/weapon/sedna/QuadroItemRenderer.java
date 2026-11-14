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
import com.hbm.util.GameTime;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionfc;

public class QuadroItemRenderer extends AnimatedGunItemRenderer {
    private static final String LABEL = ">> <<";

    public QuadroItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(
                                -2.5F * 0.8F,
                                -3.5F * 0.8F,
                                2.5F * 0.8F,
                                -1.5F * 0.8F,
                                -3F * 0.8F,
                                2.5F * 0.8F)
                        .body()
                        .size(1.75)
                        .mesh(ResourceManager.quadro)
                        .skin(ResourceManager.quadro_tex)
                        .hinge(0, -1, -1, Axis.XP, Rig.DRAW)
                        .slideZ(Rig.KICK)
                        .hinge(0, -1, -1, Axis.XP, Rig.LOAD_TILT)
                        .part("Launcher")
                        .group(
                                rockets ->
                                        rockets.shift(0, -1, 0)
                                                .hinge(
                                                        0,
                                                        3,
                                                        0,
                                                        Axis.XP,
                                                        r -> r.y(Rig.LOAD_PUSH) * 30)
                                                .slideZ(r -> r.x(Rig.LOAD_PUSH) * 3)
                                                .skin(ResourceManager.quadro_rocket_tex)
                                                .part("Rockets"))
                        .when(
                                GunRenderDefinition.Reading::steadyAim,
                                sight ->
                                        sight.custom(
                                                d ->
                                                        submitLabel(
                                                                d.collector(),
                                                                d.pose(),
                                                                LABEL,
                                                                awtColor(0F, 1F, 1F),
                                                                (p, halfWidth) -> {
                                                                    p.translate(
                                                                            -0.375F, 2.25F, 0.875F);
                                                                    p.mulPose(
                                                                            (Quaternionfc)
                                                                                    Axis.YN
                                                                                            .rotationDegrees(
                                                                                                    (float)
                                                                                                            (180D
                                                                                                                    + (GameTime
                                                                                                                                            .now()
                                                                                                                                    / 2)
                                                                                                                            % 360D)));
                                                                    p.translate(-halfWidth, 0, 0);
                                                                }),
                                                null))
                        .group(
                                flash ->
                                        flash.shift(-1, 0.75, 6.5)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .size(0.75)
                                                .flash(MuzzleFlash.NORMAL, 150, 7.5))
                        .build());
    }

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Vec3 LOAD_PUSH = RIG.vec3("RELOAD_PUSH");
        static final GunAnimationChannels.Scalar LOAD_TILT = RIG.z("RELOAD_ROTATE");
    }
}
