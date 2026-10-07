// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.NuclearTech;
import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.platform.NeoForgeServerEvents;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

public final class JoinGreeting {
    private JoinGreeting() {}

    public static void register() {
        NeoForgeServerEvents.onPlayerJoin(JoinGreeting::onPlayerJoin);
    }

    private static void onPlayerJoin(ServerPlayer player) {
        if (NeoForgeHbmConfig.runtime().enableMOTD()) player.sendSystemMessage(message());
    }

    private static Component message() {
        return Component.translatable(
                "chat.hbm.loaded",
                ModList.get()
                        .getModContainerById(NuclearTech.MOD_ID)
                        .orElseThrow()
                        .getModInfo()
                        .getVersion()
                        .toString(),
                SharedConstants.getCurrentVersion().name());
    }
}
