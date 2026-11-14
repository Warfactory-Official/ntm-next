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
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class MissileLauncherItemRenderer extends AnimatedGunItemRenderer {
    private static final String LABEL = "AUTO";

    public MissileLauncherItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(
                                -1.5F * 0.8F,
                                -1.25F * 0.8F,
                                0.5F * 0.8F,
                                -1F * 0.8F,
                                -1.25F * 0.8F,
                                0F * 0.8F)
                        .body()
                        .size(0.5)
                        .mesh(ResourceManager.missile_launcher)
                        .skin(ResourceManager.missile_launcher_tex)
                        .hinge(0, -2, -2, Axis.XP, Rig.DRAW)
                        .part("Launcher")
                        .group(
                                front ->
                                        front.hinge(0, 0.25, 1.6875, Axis.XP, Rig.OPEN)
                                                .part("Front")
                                                .group(
                                                        barrel ->
                                                                barrel.slideZ(Rig.BARREL)
                                                                        .part("Barrel"))
                                                .group(
                                                        missile ->
                                                                missile.move(Rig.MISSILE)
                                                                        .part("Missile")))
                        .when(
                                GunRenderDefinition.Reading::steadyAim,
                                sight ->
                                        sight.label(
                                                _ -> LABEL,
                                                _ ->
                                                        awtColor(
                                                                0.7F
                                                                        + Minecraft.getInstance()
                                                                                        .player
                                                                                        .getRandom()
                                                                                        .nextFloat()
                                                                                * 0.3F,
                                                                0F,
                                                                0F),
                                                0.9375,
                                                2.25,
                                                -0.5625,
                                                Axis.ZP,
                                                Axis.YP.rotationDegrees(90)))
                        .group(
                                flash ->
                                        flash.shift(0, 1, 6.75)
                                                .turn(Axis.YP, 90)
                                                .roll(90)
                                                .size(0.75)
                                                .flash(MuzzleFlash.NORMAL, 75, 7.5))
                        .build());
    }

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar BARREL = RIG.z("BARREL");
        static final GunAnimationChannels.Scalar OPEN = RIG.x("OPEN");
        static final GunAnimationChannels.Vec3 MISSILE = RIG.vec3("MISSILE");
    }
}
