// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.ore;

import java.util.List;
import net.minecraft.server.MinecraftServer;

public final class OreGeneration {
    private static volatile List<OreGenerationProfile> server = List.of();
    private static volatile List<OreGenerationProfile> client = List.of();

    private OreGeneration() {}

    public static void serverStarted(MinecraftServer minecraftServer) {
        server = OreGenerationProfiles.collect(minecraftServer);
    }

    public static List<OreGenerationProfile> serverProfiles() {
        return server;
    }

    public static List<OreGenerationProfile> clientProfiles() {
        return client;
    }

    public static void accept(List<OreGenerationProfile> profiles) {
        client = List.copyOf(profiles);
    }

    public static void disconnect() {
        client = List.of();
    }

    public static void serverStopped() {
        server = List.of();
    }
}
