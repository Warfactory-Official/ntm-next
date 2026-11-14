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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class DANIItemRenderer extends AnimatedGunItemRenderer {

    public DANIItemRenderer() {
        super(body());
    }

    private static GunRenderDefinition body() {
        var b = GunRenderDefinition.on(BioRevolverGunModel.RIG).shift(0, 0, 0.875).body();
        for (int config = 0; config < 2; config++) {
            int side = config * 2 - 1;
            var sheet =
                    config == 0
                            ? ResourceManager.dani_celestial_tex
                            : ResourceManager.dani_lunar_tex;
            b.config(
                    config,
                    c ->
                            c.group(
                                    gun -> {
                                        gun.aim(
                                                        -1.5F * 0.8F * side,
                                                        -0.75F * 0.8F,
                                                        0.8F,
                                                        0,
                                                        -3.125 / 8D,
                                                        0.25)
                                                .size(0.125);
                                        BioRevolverGunModel.gun(gun, sheet, side, -2, -2);
                                    }));
        }
        return b.build();
    }

    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    public boolean isAkimbo(LivingEntity entity) {
        return true;
    }
}
