// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionSavedData;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class SatelliteMapper extends Satellite {

    public static final MapCodec<SatelliteMapper> CODEC = MapCodec.unit(SatelliteMapper::new);
    public static final int SPOT_PLAYER_MAX_RANGE = 250;

    public SatelliteMapper() {}

    @Override
    public SatelliteType type() {
        return SatelliteType.MAPPER;
    }

    @Override
    protected void onCommandImpl(ServerLevel level, String... command) {
        if (command.length == 0) return;
        switch (command[0]) {
            case "targetloaded" ->
                    tx =
                            Boolean.toString(
                                            level.getChunkSource()
                                                            .getChunkNow(targetX >> 4, targetZ >> 4)
                                                    != null)
                                    .toUpperCase(Locale.US);
            case "getsmog" -> {
                PollutionSavedData.PollutionData data =
                        PollutionHandler.getPollutionData(
                                level, new BlockPos(targetX, 255, targetZ));
                if (data != null)
                    tx =
                            Integer.toString(
                                    (int) Math.ceil(data.pollution[PollutionType.SOOT.ordinal()]));
            }
            case "spotplayers" -> {
                List<String> names = new ArrayList<>();
                for (ServerPlayer player : level.players()) {
                    int x = (int) Math.floor(player.getX());
                    int z = (int) Math.floor(player.getZ());
                    double dx = x - targetX;
                    double dz = z - targetZ;
                    if (dx * dx + dz * dz <= SPOT_PLAYER_MAX_RANGE * SPOT_PLAYER_MAX_RANGE
                            && WorldgenHeight.lightBlocking(level.getChunk(x >> 4, z >> 4), x, z)
                                    < player.getY() + 2D) {
                        names.add(player.getGameProfile().name());
                    }
                }
                tx = names.isEmpty() ? "NONE" : String.join(";", names);
            }
            default -> {}
        }
    }
}
