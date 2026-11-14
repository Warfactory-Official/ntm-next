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

package com.hbm.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

public class ServerEvents {
    public static final List<Consumer<MinecraftServer>> TICK_PRE = new ArrayList<>();
    public static final List<Consumer<MinecraftServer>> TICK_POST = new ArrayList<>();
    public static final List<BiConsumer<ServerLevel, LevelChunk>> CHUNK_LOAD = new ArrayList<>();
    public static final List<BiConsumer<ServerLevel, LevelChunk>> CHUNK_UNLOAD = new ArrayList<>();
    public static final List<BiConsumer<Entity, ServerLevel>> ENTITY_LOAD = new ArrayList<>();
    public static final List<Consumer<MinecraftServer>> STOPPING = new ArrayList<>();
    public static final List<Consumer<ServerPlayer>> JOIN = new ArrayList<>();
    public static final List<Consumer<ServerPlayer>> DISCONNECT = new ArrayList<>();
    public static final List<Consumer<ServerPlayer>> RESPAWN = new ArrayList<>();
    public static final List<PlayerLevelChange> LEVEL_CHANGE = new ArrayList<>();

    public static void install() {
        NeoForge.EVENT_BUS.addListener(
                (ServerTickEvent.Pre event) -> {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        for (Consumer<MinecraftServer> callback : TICK_PRE) {
                            callback.accept(server);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ServerTickEvent.Post event) -> {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        for (Consumer<MinecraftServer> callback : TICK_POST) {
                            callback.accept(server);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ChunkEvent.Load event) -> {
                    if (event.getLevel() instanceof ServerLevel level
                            && event.getChunk() instanceof LevelChunk chunk) {
                        for (BiConsumer<ServerLevel, LevelChunk> callback : CHUNK_LOAD) {
                            callback.accept(level, chunk);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ChunkEvent.Unload event) -> {
                    if (event.getLevel() instanceof ServerLevel level
                            && event.getChunk() instanceof LevelChunk chunk) {
                        for (BiConsumer<ServerLevel, LevelChunk> callback : CHUNK_UNLOAD) {
                            callback.accept(level, chunk);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerLoggedInEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        for (Consumer<ServerPlayer> callback : JOIN) {
                            callback.accept(player);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerLoggedOutEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        for (Consumer<ServerPlayer> callback : DISCONNECT) {
                            callback.accept(player);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerRespawnEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        for (Consumer<ServerPlayer> callback : RESPAWN) {
                            callback.accept(player);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerChangedDimensionEvent event) -> {
                    if (!(event.getEntity() instanceof ServerPlayer player)) return;
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server == null) return;
                    ServerLevel origin = server.getLevel(event.getFrom());
                    ServerLevel destination = server.getLevel(event.getTo());
                    if (origin == null || destination == null) return;
                    for (PlayerLevelChange callback : LEVEL_CHANGE) {
                        callback.accept(player, origin, destination);
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ServerStoppingEvent event) -> {
                    for (Consumer<MinecraftServer> callback : STOPPING) {
                        callback.accept(event.getServer());
                    }
                });
    }

    public static @Nullable MinecraftServer getCurrentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static void dispatchEntityLoad(Entity entity, ServerLevel level) {
        for (BiConsumer<Entity, ServerLevel> callback : ENTITY_LOAD) callback.accept(entity, level);
    }
}
