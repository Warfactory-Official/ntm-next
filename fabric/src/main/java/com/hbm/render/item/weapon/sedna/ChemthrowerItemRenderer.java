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
import net.minecraft.world.item.ItemStack;

public class ChemthrowerItemRenderer extends AnimatedGunItemRenderer {

    public ChemthrowerItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-2.5F * 0.8F, -2.5F * 0.8F, 2.5F * 0.8F, 0, -4.375 / 8D, 1)
                        .body()
                        .size(0.75)
                        .mesh(ResourceManager.chemthrower)
                        .skin(ResourceManager.chemthrower_tex)
                        .hinge(0, -2, -4, Axis.XN, Rig.DRAW)
                        .turn(Axis.YP, 90)
                        .part("Gun")
                        .part("Hose")
                        .part("Nozzle")
                        .hinge(
                                0,
                                0.875,
                                1.75,
                                Axis.XP,
                                r -> {
                                    var mag = GunRenderDefinition.magazine(r);
                                    double fill =
                                            (double) mag.getAmount(r.stack(), r.inventory())
                                                    / (double) mag.getCapacity(r.stack());
                                    return 135 - fill * 270;
                                })
                        .part("Gauge")
                        .build());
    }

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
    }
}
