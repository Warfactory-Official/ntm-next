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

package com.hbm.saveddata.satellites;

import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.DeathBlastEntity;
import com.hbm.util.ChunkUtil;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public final class DeathRaySatellite extends ChargedSatellite {

    public static final int CHARGE = 6000;
    public static final MapCodec<DeathRaySatellite> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .optionalFieldOf("lastShot", 0L)
                                                    .forGetter(sat -> sat.firedAt))
                                    .apply(i, DeathRaySatellite::new));

    public DeathRaySatellite() {
        this(0L);
    }

    private DeathRaySatellite(long firedAt) {
        super(CHARGE, firedAt);
    }

    @Override
    public SatelliteType kind() {
        return SatelliteType.DEATH_RAY;
    }

    @Override
    protected void kindCommand(ServerLevel level, String[] tokens) {
        super.kindCommand(level, tokens);
        if (tokens[0].equals("fire")) strike(level, targetX(), targetZ());
    }

    @Override
    public void designate(ServerLevel level, Player player, int x, int y, int z) {
        aim(x, z);
        changed();
        strike(level, x, z);
    }

    public void strike(ServerLevel level, int x, int z) {
        if (!discharge(level)) return;
        ChunkUtil.loadTickingForEntity(level, new ChunkPos(x >> 4, z >> 4));
        int y = WorldgenHeight.lightBlocking(level.getChunk(x >> 4, z >> 4), x, z);
        var blast = new DeathBlastEntity(ModEntities.LASER_BLAST.get(), level);
        blast.setPos(x, y, z);
        level.addFreshEntity(blast);
    }
}
