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

package com.hbm.saveddata.satellites;

import com.hbm.itempool.ItemPool;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;

public class MinerSatellite extends Satellite {
    public static final double SPEED = 1D / 18_000;
    private static final int PROGRESS_SAVE_PERIOD = 1200;
    public static final MapCodec<MinerSatellite> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.DOUBLE
                                                    .optionalFieldOf("progress", 0D)
                                                    .forGetter(sat -> sat.progress))
                                    .apply(i, MinerSatellite::new));

    public double progress;

    public MinerSatellite() {
        this(0D);
    }

    protected MinerSatellite(double progress) {
        this.progress = progress;
    }

    @Override
    public SatelliteType kind() {
        return SatelliteType.MINER;
    }

    @Override
    public void tick(ServerLevel level) {
        var cargo = cargo();
        if (!cargo.isEmpty()) return;
        progress += SPEED;
        if (progress >= 1D) {
            progress = 0D;
            String pool = kind().cargo();
            var random = level.getRandom();
            for (int stacks = 10 + random.nextInt(6); stacks > 0; stacks--)
                cargo.add(ItemPool.getStack(pool, random));
            changed();
        }
        if (level.getGameTime() % PROGRESS_SAVE_PERIOD == 0) changed();
    }
}
