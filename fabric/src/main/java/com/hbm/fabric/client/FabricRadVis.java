// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.client;

import com.hbm.client.ClientRegistry;
import com.hbm.handler.radiation.RadVisCommand;
import com.hbm.handler.radiation.RadVisOverlay;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

final class FabricRadVis {
    private FabricRadVis() {}

    static void register() {
        HudElementRegistry.addLast(
                ClientRegistry.RADVIS_HUD_ID,
                (graphics, deltaTracker) -> ClientRegistry.renderRadVisHud(graphics));
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> RadVisOverlay.clear());
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> dispatcher.register(RadVisCommand.build()));
    }
}
