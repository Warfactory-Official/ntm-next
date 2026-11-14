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
import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;

public class FlamerItemRenderer extends AnimatedGunItemRenderer {

    public FlamerItemRenderer(Identifier texture) {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.5F * 0.8F, -1.5F * 0.8F, 2.75F * 0.8F, 0, -4.625 / 8D, 0.25)
                        .body()
                        .size(0.375)
                        .mesh(ResourceManager.flamethrower)
                        .skin(texture)
                        .hinge(0, 2, -6, Axis.XN, Rig.DRAW)
                        .hinge(0, 1, 0, Axis.ZP, Rig.ROLL)
                        .group(
                                gun ->
                                        gun.apply(Rig.GUN)
                                                .part("Gun")
                                                .when(
                                                        r ->
                                                                r.stack().getItem()
                                                                        == ModItems
                                                                                .GUN_FLAMER_DAYBREAKER
                                                                                .get(),
                                                        shield -> shield.part("HeatShield")))
                        .group(tank -> tank.apply(Rig.TANK).part("Tank"))
                        .group(
                                gauge ->
                                        gauge.apply(Rig.GAUGE)
                                                .hinge(
                                                        1.25,
                                                        1.25,
                                                        0,
                                                        Axis.ZP,
                                                        r -> {
                                                            var mag =
                                                                    GunRenderDefinition.magazine(r);
                                                            return -135
                                                                    + (mag.getAmount(
                                                                                    r.stack(),
                                                                                    r.inventory())
                                                                            * 270D
                                                                            / mag.getCapacity(
                                                                                    r.stack()));
                                                        })
                                                .part("Gauge"))
                        .build());
        Rig.RIG.requireParts("flamethrower", ResourceManager.flamethrower_anim);
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar ROLL = RIG.z("ROTATE");
        static final GunAnimationChannels.Part GUN = RIG.part("Gun");
        static final GunAnimationChannels.Part TANK = RIG.part("Tank");
        static final GunAnimationChannels.Part GAUGE = RIG.part("Gauge");
    }
}
