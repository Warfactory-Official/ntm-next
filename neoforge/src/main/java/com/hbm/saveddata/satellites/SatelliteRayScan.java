// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class SatelliteRayScan extends Satellite {

    public static final MapCodec<SatelliteRayScan> CODEC = MapCodec.unit(SatelliteRayScan::new);
    public static final int MAX_SCAN_RANGE = 250;

    public List<SatelliteRayEvents.RayEvent> cachedResults = new ArrayList<>();

    @Override
    public SatelliteType type() {
        return SatelliteType.RAY_SCAN;
    }

    @Override
    protected void onCommandImpl(ServerLevel level, String... command) {
        if (command.length == 0) return;
        switch (command[0]) {
            case "survey" -> {
                cachedResults.clear();
                for (Map.Entry<BlockPos, SatelliteRayEvents.RayEvent> entry :
                        SatelliteRayEvents.events(level).entrySet()) {
                    BlockPos pos = entry.getKey();

                    long dx = (long) pos.getX() - targetX;
                    long dz = (long) pos.getZ() - targetZ;
                    if (dx * dx + dz * dz <= (long) MAX_SCAN_RANGE * MAX_SCAN_RANGE) {
                        cachedResults.add(entry.getValue());
                    }
                }
            }
            case "count" -> tx = Integer.toString(cachedResults.size());
            case "getinfo" -> {
                if (command.length != 2) return;
                SatelliteRayEvents.RayEvent event = getEventFromIndex(command[1]);
                tx = event == null ? "" : event.info();
            }
            case "getposition" -> {
                if (command.length != 2) return;
                SatelliteRayEvents.RayEvent event = getEventFromIndex(command[1]);
                tx = event == null ? "" : event.x() + ";" + event.z();
            }
            default -> {}
        }
    }

    public SatelliteRayEvents.@Nullable RayEvent getEventFromIndex(String indexText) {
        if (cachedResults.isEmpty()) return null;
        int index = IRORInteractive.parseInt(indexText, 1, cachedResults.size()) - 1;
        return cachedResults.get(index);
    }
}
