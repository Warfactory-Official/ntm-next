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

public class TeslaCannonItemRenderer extends AnimatedGunItemRenderer {

    public TeslaCannonItemRenderer() {
        super(body());
    }

    private static GunRenderDefinition body() {
        var b =
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(
                                -1.75F * 0.8F,
                                -0.5F * 0.8F,
                                1.75F * 0.8F,
                                -1.3125F * 0.8F,
                                0F * 0.8F,
                                -0.5F * 0.8F)
                        .body()
                        .size(0.75)
                        .mesh(ResourceManager.tesla_cannon)
                        .skin(ResourceManager.tesla_cannon_tex)
                        .hinge(0, -2, -2, Axis.XP, Rig.DRAW)
                        .slideZ(Rig.KICK)
                        .spin(Axis.XP, r -> r.get(Rig.KICK) * 2)
                        .part("Gun")
                        .part("Extension")
                        .group(cog -> cog.hinge(0, -1.625, 0, Axis.ZP, Rig.COG).part("Cog"))
                        .group(chain -> belt(chain.hinge(0, -1.625, 0, Axis.ZP, Rig.COG), 0))
                        .group(
                                yomi ->
                                        yomi.move(Rig.YOMI)
                                                .turn(Axis.YP, 135)
                                                .size(
                                                        r -> r.x(Rig.SQUEEZE),
                                                        r -> r.y(Rig.SQUEEZE),
                                                        r -> r.z(Rig.SQUEEZE))
                                                .mesh(ResourceManager.yomi)
                                                .skin(ResourceManager.yomi_tex)
                                                .whole());
        return b.build();
    }

    private static void belt(GunRenderDefinition.Builder b, int index) {
        if (index == 8) return;
        b.when(
                r -> loaded(r) > index,
                c -> {
                    c.part("Capacitor");
                    if (index < 4) {
                        c.about(0, -1.625, 0, a -> a.turn(Axis.ZN, 22.5));
                    } else {
                        if (index == 4)
                            c.hinge(0, -1.625, 0, Axis.ZN, Rig.COG)
                                    .slideX(r -> -r.get(Rig.COG) * 0.5 / 22.5);
                        c.shift(0.5, 0, 0);
                    }
                    belt(c, index + 1);
                });
    }

    private static int loaded(GunRenderDefinition.Reading r) {
        var supply = r.inventory();
        int carried =
                supply == null ? 0 : GunRenderDefinition.magazine(r).getAmount(r.stack(), supply);
        return Math.max((int) r.get(Rig.COUNT), carried);
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar COG = RIG.z("CYCLE");
        static final GunAnimationChannels.Scalar COUNT = RIG.x("COUNT");
        static final GunAnimationChannels.Vec3 YOMI = RIG.vec3("YOMI");
        static final GunAnimationChannels.Vec3 SQUEEZE = RIG.vec3("SQUEEZE");
    }
}
