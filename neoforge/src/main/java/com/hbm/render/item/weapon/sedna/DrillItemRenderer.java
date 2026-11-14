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
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemStack;

public class DrillItemRenderer extends AnimatedGunItemRenderer {

    public DrillItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(
                                -1.25F * 0.8F,
                                -1.75F * 0.8F,
                                1.75F * 0.8F,
                                -1F * 0.8F,
                                -1.75F * 0.8F,
                                1.25F * 0.8F)
                        .body()
                        .size(0.375)
                        .mesh(ResourceManager.drill)
                        .skin(ResourceManager.drill_tex)
                        .spin(Axis.YP, r -> 15 * (1 - r.get(Rig.EXTEND) * 0.5))
                        .spin(Axis.XP, r -> -10 * (1 - r.get(Rig.EXTEND) * 0.5))
                        .about(
                                0,
                                2,
                                -6,
                                a ->
                                        a.spin(Axis.YP, r -> r.get(Rig.DRAW) * -45)
                                                .spin(Axis.XP, r -> r.get(Rig.DRAW) * -20))
                        .spin(Axis.XP, Rig.TILT)
                        .slideZ(Rig.EXTEND)
                        .part("Base")
                        .group(
                                gauge ->
                                        gauge.about(
                                                        1,
                                                        2.0625,
                                                        -1.75,
                                                        a ->
                                                                a.turn(Axis.XP, 45)
                                                                        .spin(
                                                                                Axis.ZP,
                                                                                r -> {
                                                                                    var mag =
                                                                                            GunRenderDefinition
                                                                                                    .magazine(
                                                                                                            r);
                                                                                    double fill =
                                                                                            (double)
                                                                                                            mag
                                                                                                                    .getAmount(
                                                                                                                            r
                                                                                                                                    .stack(),
                                                                                                                            null)
                                                                                                    / (double)
                                                                                                            mag
                                                                                                                    .getCapacity(
                                                                                                                            r
                                                                                                                                    .stack());
                                                                                    return -135
                                                                                            + fill
                                                                                                    * 270;
                                                                                })
                                                                        .turn(Axis.XP, -45))
                                                .part("Gauge"))
                        .group(piston -> piston.slideY(r -> stroke(r, 0)).part("Piston1"))
                        .group(
                                piston ->
                                        piston.slideY(r -> stroke(r, Math.PI * 2D / 3D))
                                                .part("Piston2"))
                        .group(
                                piston ->
                                        piston.slideY(r -> stroke(r, Math.PI * 4D / 3D))
                                                .part("Piston3"))
                        .group(back -> back.spin(Axis.ZN, Rig.BIT).part("DrillBack"))
                        .group(front -> front.spin(Axis.ZP, Rig.BIT).part("DrillFront"))
                        .build());
    }

    private static double stroke(GunRenderDefinition.Reading r, double phase) {
        return Math.sin(r.get(Rig.BIT) * 5 * Math.PI / 180 + phase) * 0.125 - 0.125;
    }

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return GunBaseNTItem.getIsAiming(stack) ? 0F : -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar EXTEND = RIG.x("DEPLOY");
        static final GunAnimationChannels.Scalar TILT = RIG.x("LIFT");
        static final GunAnimationChannels.Scalar BIT = RIG.x("SPIN");

        static {
            RIG.carries("SPEED");
        }
    }
}
