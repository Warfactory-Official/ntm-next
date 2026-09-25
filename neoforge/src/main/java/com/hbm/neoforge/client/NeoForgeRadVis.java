// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.handler.radiation.RadVisCommand;
import com.hbm.handler.radiation.RadVisOverlay;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeRadVis {
    private NeoForgeRadVis() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> RadVisOverlay.clear());
        NeoForge.EVENT_BUS.addListener(
                (RegisterClientCommandsEvent event) ->
                        event.getDispatcher().register(RadVisCommand.build()));
    }
}
