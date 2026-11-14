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
