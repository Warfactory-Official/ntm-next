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

package com.hbm.handler;

import com.hbm.NuclearTech;
import com.hbm.client.UpdateChecker;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.platform.ServerEvents;
import java.net.URI;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class JoinGreeting {
    private static final URI CONTINUUM_URL =
            URI.create("https://www.curseforge.com/minecraft/mc-mods/ntm-continuum");

    private JoinGreeting() {}

    public static void register() {
        final Consumer<ServerPlayer> callback = JoinGreeting::onPlayerJoin;
        ServerEvents.JOIN.add(callback);
    }

    private static void onPlayerJoin(ServerPlayer player) {
        if (((RuntimeConfig) HbmConfig.RUNTIME).enableMOTD()) player.sendSystemMessage(message());
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
                SharedConstants.getCurrentVersion().name(),
                UpdateChecker.link(Component.literal("NTM-Continuum"), CONTINUUM_URL));
    }
}
