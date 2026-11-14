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
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import net.minecraft.resources.Identifier;

public class MareslegItemRenderer extends AnimatedGunItemRenderer {

    public MareslegItemRenderer(Identifier texture) {
        super(
                GunRenderDefinition.on(MareslegGunModel.RIG)
                        .shift(0, 0, 0.875)
                        .aim(-1.25F * 0.8F, -1F * 0.8F, 2F * 0.8F, 0, -3.875 / 8D, 1)
                        .body()
                        .size(0.375)
                        .group(
                                gun ->
                                        MareslegGunModel.gun(
                                                gun, texture, MareslegItemRenderer::sawn))
                        .build());
    }

    private static boolean sawn(GunRenderDefinition.Reading r) {
        return r.stack().getItem() == ModItems.GUN_MARESLEG_BROKEN.get()
                || XWeaponModManager.hasUpgrade(r.stack(), 0, XWeaponModManager.ID_SAWED_OFF);
    }

    @Override
    protected float idleTurnMagnitude() {
        return -0.5F;
    }
}
