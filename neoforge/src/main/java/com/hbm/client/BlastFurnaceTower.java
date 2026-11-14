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

package com.hbm.client;

import com.hbm.blockentity.machine.BlastFurnaceBlockEntity;
import com.hbm.particle.CoolingTowerParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class BlastFurnaceTower {

    private BlastFurnaceTower() {}

    public static void tick(BlastFurnaceBlockEntity be) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return;

        BlockPos pos = be.getBlockPos();
        double x = pos.getX() + 0.5, y = pos.getY() + 7, z = pos.getZ() + 0.5;
        if (me.getEyePosition().distanceToSqr(x, y, z) >= 100 * 100) return;

        CoolingTowerParticleOptions opts =
                new CoolingTowerParticleOptions.Builder()
                        .setLift(10F)
                        .setBaseScale(0.25F)
                        .setMaxScale(2.5F)
                        .setLife(100 + be.getLevel().getRandom().nextInt(20))
                        .setColor(0x202020)
                        .build();
        be.getLevel().addParticle(opts, true, false, x, y, z, 0, 0, 0);
    }
}
