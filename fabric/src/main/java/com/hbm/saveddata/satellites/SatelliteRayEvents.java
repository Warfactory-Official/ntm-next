// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class SatelliteRayEvents {

    public static final String ARC_FLASH = "ARC_FLASH";
    public static final String NEUTRON_EMISSION = "NEUTRON_EMISSION";
    public static final String HIGH_ENERGY_PARTICLES = "HIGH_ENERGY_PARTICLES";
    public static final String RADAR_WAVES = "RADAR_WAVES";
    public static final String RADIO_WAVES = "RADIO_WAVES";
    private static final Map<ResourceKey<Level>, LinkedHashMap<BlockPos, RayEvent>> EVENTS =
            new HashMap<>();

    private SatelliteRayEvents() {}

    public static void report(ServerLevel level, BlockPos pos, String info, int lifetime) {
        EVENTS.computeIfAbsent(level.dimension(), ignored -> new LinkedHashMap<>())
                .put(
                        pos.immutable(),
                        new RayEvent(level.getGameTime() + lifetime, pos.getX(), pos.getZ(), info));
    }

    public static Map<BlockPos, RayEvent> events(ServerLevel level) {
        Map<BlockPos, RayEvent> entries = EVENTS.get(level.dimension());
        return entries == null ? Map.of() : entries;
    }

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getGameTime() % 20 != 10) continue;
            LinkedHashMap<BlockPos, RayEvent> entries = EVENTS.get(level.dimension());
            if (entries != null) {
                entries.entrySet()
                        .removeIf(entry -> level.getGameTime() > entry.getValue().expiresOn());
                if (entries.isEmpty()) EVENTS.remove(level.dimension());
            }
        }
    }

    public static void clear() {
        EVENTS.clear();
    }

    public record RayEvent(long expiresOn, int x, int z, String info) {}
}
