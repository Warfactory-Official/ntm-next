// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class SatelliteDetector extends Satellite {

    public static final MapCodec<SatelliteDetector> CODEC = MapCodec.unit(SatelliteDetector::new);
    public static final int DURATION_LOW = 15 * 20;
    public static final int DURATION_MEDIUM = 20 / 2;
    public static final int DURATION_HIGH = 60 * 20;
    public static final double INACCURACY_LOW = 10_000D;
    public static final double INACCURACY_MEDIUM = 2_500D;
    public static final double INACCURACY_HIGH = 500D;
    private static final List<RadiationBurst> BURSTS = new ArrayList<>();

    public List<RadiationBurst> cachedResults = new ArrayList<>();

    @Override
    public SatelliteType type() {
        return SatelliteType.DETECTOR;
    }

    @Override
    protected void onCommandImpl(ServerLevel level, String... command) {
        if (command.length == 0) return;
        switch (command[0]) {
            case "survey" -> {
                cachedResults.clear();
                for (RadiationBurst burst : BURSTS) {
                    if (burst.dimension().equals(level.dimension())) cachedResults.add(burst);
                }
            }
            case "count" -> tx = Integer.toString(cachedResults.size());
            case "gettype" -> {
                if (command.length != 2) return;
                RadiationBurst burst = getBurstFromIndex(command[1]);
                tx = burst == null ? "" : burst.intensity().name();
            }
            case "getposition" -> {
                if (command.length != 2) return;
                RadiationBurst burst = getBurstFromIndex(command[1]);
                tx = burst == null ? "" : burst.x() + ";" + burst.z();
            }
            default -> {}
        }
    }

    public @Nullable RadiationBurst getBurstFromIndex(String indexText) {
        if (cachedResults.isEmpty()) return null;
        int index = IRORInteractive.parseInt(indexText, 1, cachedResults.size()) - 1;
        return cachedResults.get(index);
    }

    public static void reportEvent(
            ServerLevel level, int lifetime, BurstIntensity intensity, double x, double z) {
        int reportedX = (int) Math.floor(x);
        int reportedZ = (int) Math.floor(z);
        double inaccuracy =
                switch (intensity) {
                    case LOW -> INACCURACY_LOW;
                    case MEDIUM -> INACCURACY_MEDIUM;
                    case HIGH -> INACCURACY_HIGH;
                };
        reportedX += Mth.clamp(level.getRandom().nextGaussian(), -1D, 1D) * inaccuracy;
        reportedZ += Mth.clamp(level.getRandom().nextGaussian(), -1D, 1D) * inaccuracy;
        BURSTS.add(
                new RadiationBurst(
                        level.dimension(),
                        level.getGameTime() + lifetime,
                        intensity,
                        reportedX,
                        reportedZ));
    }

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            BURSTS.removeIf(
                    burst ->
                            burst.dimension().equals(level.dimension())
                                    && level.getGameTime() > burst.expiresOn());
        }
    }

    public static void clear() {
        BURSTS.clear();
    }

    public enum BurstIntensity {
        LOW,
        MEDIUM,
        HIGH
    }

    public record RadiationBurst(
            ResourceKey<Level> dimension, long expiresOn, BurstIntensity intensity, int x, int z) {}
}
