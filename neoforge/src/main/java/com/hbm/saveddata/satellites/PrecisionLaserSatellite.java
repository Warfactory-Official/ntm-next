/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.OrbitalLaserEntity;
import com.hbm.util.ChunkUtil;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public final class PrecisionLaserSatellite extends ChargedSatellite {

    private static final int CHARGE = 100;
    private static final int NO_ENTITY = -1;
    private static final long TRACKING_RANGE_SQ = 1000L * 1000L;
    public static final MapCodec<PrecisionLaserSatellite> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .optionalFieldOf("lastShot", 0L)
                                                    .forGetter(sat -> sat.firedAt),
                                            Codec.INT
                                                    .optionalFieldOf("targetedEntity", NO_ENTITY)
                                                    .forGetter(sat -> sat.trackedEntity))
                                    .apply(i, PrecisionLaserSatellite::new));

    public int trackedEntity;

    public PrecisionLaserSatellite() {
        this(0L, NO_ENTITY);
    }

    private PrecisionLaserSatellite(long firedAt, int trackedEntity) {
        super(CHARGE, firedAt);
        this.trackedEntity = trackedEntity;
    }

    @Override
    public SatelliteType kind() {
        return SatelliteType.PRECISION_LASER;
    }

    @Override
    protected void kindCommand(ServerLevel level, String[] tokens) {
        super.kindCommand(level, tokens);
        switch (tokens[0]) {
            case "fire" -> fire(level);
            case "setentitytarget" -> {
                if (tokens.length == 2) trackedEntity = RadioCommandParser.intParam(tokens[1]);
            }
            default -> {}
        }
    }

    @Override
    public void designate(ServerLevel level, Player player, int x, int y, int z) {
        aim(x, z);
        changed();
        strike(level, x, z);
    }

    private void fire(ServerLevel level) {
        if (trackedEntity != NO_ENTITY) {
            var entity = level.getEntity(trackedEntity);
            trackedEntity = NO_ENTITY;
            if (entity == null || entity.isRemoved() || entity.isSpectator()) return;
            long dx = (long) Mth.floor(entity.getX()) - targetX();
            long dz = (long) Mth.floor(entity.getZ()) - targetZ();
            if (dx * dx + dz * dz <= TRACKING_RANGE_SQ) {
                RandomSource random = level.getRandom();
                double jitterX = random.nextDouble() * 0.05D - 0.025D;
                double jitterY = random.nextDouble() * 0.05D - 0.025D;
                double jitterZ = random.nextDouble() * 0.05D - 0.025D;
                strikeAt(
                        level,
                        entity.getX() + jitterX,
                        entity.getY() + jitterY,
                        entity.getZ() + jitterZ);
                return;
            }
        }
        strike(level, targetX(), targetZ());
    }

    private void strike(ServerLevel level, int x, int z) {
        if (!charged(level)) return;
        ChunkUtil.loadTickingForEntity(level, new ChunkPos(x >> 4, z >> 4));
        int y = WorldgenHeight.lightBlocking(level.getChunk(x >> 4, z >> 4), x, z);
        strikeAt(level, x + 0.5D, y, z + 0.5D);
    }

    private void strikeAt(ServerLevel level, double x, double y, double z) {
        if (!discharge(level)) return;
        var laser = new OrbitalLaserEntity(ModEntities.ORBITAL_LASER.get(), level);
        laser.setPos(x, y, z);
        laser.explode();
        level.addFreshEntity(laser);
    }
}
