// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.NuclearTech;
import com.hbm.config.FabricHbmConfig;
import com.hbm.platform.FabricServerEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class JoinGreeting {
    private JoinGreeting() {}

    public static void register() {
        FabricServerEvents.onPlayerJoin(JoinGreeting::onPlayerJoin);
    }

    private static void onPlayerJoin(ServerPlayer player) {
        if (FabricHbmConfig.runtime().enableMOTD()) player.sendSystemMessage(message());
    }

    private static Component message() {
        return Component.translatable(
                "chat.hbm.loaded",
                FabricLoader.getInstance()
                        .getModContainer(NuclearTech.MOD_ID)
                        .orElseThrow()
                        .getMetadata()
                        .getVersion()
                        .getFriendlyString(),
                SharedConstants.getCurrentVersion().name());
    }
}
