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

public class TauItemRenderer extends AnimatedGunItemRenderer {

    public TauItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(
                                -1.75F * 0.8F,
                                -1.75F * 0.8F,
                                3.5F * 0.8F,
                                -1.75F * 0.8F,
                                -1.75F * 0.8F,
                                3.5F * 0.8F)
                        .body()
                        .size(0.75)
                        .mesh(ResourceManager.tau)
                        .skin(ResourceManager.tau_tex)
                        .hinge(0, -1, -4, Axis.XP, Rig.DRAW)
                        .slideZ(Rig.KICK)
                        .hinge(0, 0, -2, Axis.XP, r -> r.get(Rig.KICK) * 5)
                        .part("Body")
                        .group(rotor -> rotor.hinge(0, -0.25, 0, Axis.ZP, Rig.SPOOL).part("Rotor"))
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
        static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
        static final GunAnimationChannels.Scalar SPOOL = RIG.z("ROTATE");
    }
}
