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

import com.hbm.items.ModItems;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;

public class MinigunItemRenderer extends AnimatedGunItemRenderer {

    public MinigunItemRenderer(Identifier texture) {
        super(
                MinigunModel.frame(
                                GunRenderDefinition.on(MinigunModel.RIG)
                                        .shift(0, 0, 0.875)
                                        .aim(
                                                -1.75F * 0.8F,
                                                -1.75F * 0.8F,
                                                3.5F * 0.8F,
                                                0,
                                                -6.25 / 8D,
                                                1)
                                        .body()
                                        .size(0.375),
                                texture)
                        .part("Gun")
                        .part("Grip")
                        .group(barrels -> barrels.spin(Axis.ZP, MinigunModel.SPOOL).part("Barrels"))
                        .group(
                                smoke ->
                                        smoke.shift(-2, 1.25, -3.5)
                                                .turn(Axis.YP, 45)
                                                .size(0.5)
                                                .smoke(0.5))
                        .group(
                                flash ->
                                        flash.shift(0, 0, 12)
                                                .turn(Axis.YP, 90)
                                                .either(
                                                        r ->
                                                                r.stack().getItem()
                                                                        == ModItems
                                                                                .GUN_MINIGUN_LACUNAE
                                                                                .get(),
                                                        beam ->
                                                                beam.flash(
                                                                                MuzzleFlash.LASER,
                                                                                50,
                                                                                1,
                                                                                0xff00ff)
                                                                        .shift(0, 0, -0.25)
                                                                        .flash(
                                                                                MuzzleFlash.LASER,
                                                                                50,
                                                                                0.5,
                                                                                0xff0080),
                                                        fire ->
                                                                fire.shift(0, 0.5, 0)
                                                                        .roll(90)
                                                                        .size(1.5)
                                                                        .flash(
                                                                                MuzzleFlash.NORMAL,
                                                                                50,
                                                                                7.5)))
                        .build());
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }
}
