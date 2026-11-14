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

import com.hbm.client.render.RenderTextures;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.items.weapon.sedna.impl.GunNI4NIItem;
import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;
import java.util.function.ToIntFunction;

public class NI4NIItemRenderer extends AnimatedGunItemRenderer {

    public NI4NIItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 1)
                        .aim(-1.0F * 0.8F, -1F * 0.8F, 0.8F, 0, -5 / 8D, 0.125)
                        .body()
                        .size(0.3125)
                        .mesh(ResourceManager.n_i_4_n_i)
                        .skin(
                                r -> GunNI4NIItem.getColors(r.stack()) == null,
                                ResourceManager.n_i_4_n_i_tex,
                                ResourceManager.n_i_4_n_i_greyscale_tex)
                        .hinge(0, 0, -2.25, Axis.XP, Rig.DRAW)
                        .hinge(0, -1, -6, Axis.XP, Rig.KICK)
                        .group(NI4NIItemRenderer::gun)
                        .build());
    }

    private static void gun(GunRenderDefinition.Builder b) {
        b.part("FrameDark", tint(0))
                .part("Grip", tint(2))
                .part("FrameLight", tint(1))
                .group(
                        drum ->
                                drum.hinge(0, 1.1875, 0, Axis.ZP, Rig.CYLINDER)
                                        .part("Cylinder", tint(1))
                                        .glow("CylinderHighlights"))
                .glow("Barrel");
        var mesh = ResourceManager.n_i_4_n_i;
        int[] coins = mesh.partIds("Coin1", "Coin2", "Coin3", "Coin4");
        b.custom(
                d -> {
                    int count = GunNI4NIItem.getCoinCount(d.stack());
                    d.collector()
                            .submitCustomGeometry(
                                    d.pose(),
                                    WeaponRenderTypes.UNTEXTURED,
                                    (p, buffer) -> {
                                        for (int c = 0; c < coins.length; c++) {
                                            if (count > 3 - c)
                                                mesh.renderPart(
                                                        p,
                                                        buffer,
                                                        FULL_BRIGHT,
                                                        coin(count, c),
                                                        coins[c]);
                                        }
                                    });
                },
                (r, pose, target) -> {
                    int count = GunNI4NIItem.getCoinCount(r.stack());
                    for (int c = 0; c < coins.length; c++) {
                        if (count > 3 - c)
                            target.part(
                                    pose,
                                    mesh,
                                    coins[c],
                                    RenderTextures.WHITE,
                                    coin(count, c),
                                    true);
                    }
                });
        b.group(
                flash ->
                        flash.shift(0, 0.75, 4)
                                .turn(Axis.YP, 90)
                                .roll(90)
                                .size(0.125)
                                .flash(MuzzleFlash.LASER, 75, 7.5, 0xFFFFFF));
    }

    private static ToIntFunction<GunRenderDefinition.Reading> tint(int index) {
        return r -> {
            int[] colors = GunNI4NIItem.getColors(r.stack());
            return colors == null ? 0xFFFFFFFF : 0xFF000000 | colors[index];
        };
    }

    private static int coin(int count, int c) {
        return 0xFF00FF00 | (count > 7 - c ? 0xFF0000 : 0);
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Scalar KICK = RIG.x("RECOIL");
        static final GunAnimationChannels.Scalar CYLINDER = RIG.z("DRUM");
    }
}
