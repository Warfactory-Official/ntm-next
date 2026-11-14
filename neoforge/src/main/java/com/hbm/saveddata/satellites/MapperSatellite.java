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

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

public final class MapperSatellite extends Satellite {

    public static final MapCodec<MapperSatellite> CODEC = MapCodec.unit(MapperSatellite::new);
    private static final long SPOTTING_RANGE_SQ = 250L * 250L;
    private static final int SMOG_SAMPLE_Y = 255;

    @Override
    public SatelliteType kind() {
        return SatelliteType.MAPPER;
    }

    @Override
    protected void kindCommand(ServerLevel level, String[] tokens) {
        switch (tokens[0]) {
            case "targetloaded" ->
                    reply(
                            level.getChunkSource().getChunkNow(targetX() >> 4, targetZ() >> 4)
                                            != null
                                    ? "TRUE"
                                    : "FALSE");
            case "getsmog" -> {
                var reading = new float[PollutionType.COUNT];
                if (PollutionHandler.sample(
                        level, new BlockPos(targetX(), SMOG_SAMPLE_Y, targetZ()), reading)) {
                    reply(Integer.toString((int) Math.ceil(reading[PollutionType.SOOT.ordinal()])));
                }
            }
            case "spotplayers" -> reply(spotPlayers(level));
            default -> {}
        }
    }

    private String spotPlayers(ServerLevel level) {
        StringBuilder names = null;
        for (var player : level.players()) {
            if (player.isSpectator()) continue;
            int x = Mth.floor(player.getX());
            int z = Mth.floor(player.getZ());
            long dx = (long) x - targetX();
            long dz = (long) z - targetZ();
            if (dx * dx + dz * dz > SPOTTING_RANGE_SQ) continue;
            if (!(WorldgenHeight.lightBlocking(level.getChunk(x >> 4, z >> 4), x, z)
                    < player.getY() + 2D)) continue;
            if (names == null) names = new StringBuilder();
            else names.append(';');
            names.append(player.getGameProfile().name());
        }
        return names == null ? "NONE" : names.toString();
    }
}
