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

import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class ServerEvents {
    public static final List<Consumer<MinecraftServer>> TICK_PRE = new ArrayList<>();
    public static final List<Consumer<MinecraftServer>> TICK_POST = new ArrayList<>();
    public static final List<BiConsumer<ServerLevel, LevelChunk>> CHUNK_LOAD = new ArrayList<>();
    public static final List<BiConsumer<ServerLevel, LevelChunk>> CHUNK_UNLOAD = new ArrayList<>();
    public static final List<Consumer<MinecraftServer>> STOPPING = new ArrayList<>();
    public static final List<Consumer<ServerPlayer>> JOIN = new ArrayList<>();
    public static final List<Consumer<ServerPlayer>> DISCONNECT = new ArrayList<>();
    public static final List<Consumer<ServerPlayer>> RESPAWN = new ArrayList<>();
    public static final List<PlayerLevelChange> LEVEL_CHANGE = new ArrayList<>();
    private static final Identifier RESTORED_PHASE = RegistryUtil.id("restored");
    private static volatile @Nullable MinecraftServer server;

    public static void install() {
        ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
        ServerTickEvents.START_SERVER_TICK.register(
                s -> {
                    for (Consumer<MinecraftServer> callback : TICK_PRE) {
                        callback.accept(s);
                    }
                });
        ServerTickEvents.END_SERVER_TICK.register(
                s -> {
                    for (Consumer<MinecraftServer> callback : TICK_POST) {
                        callback.accept(s);
                    }
                });
        ServerChunkEvents.CHUNK_LOAD.register(
                (level, chunk, generated) -> {
                    for (BiConsumer<ServerLevel, LevelChunk> callback : CHUNK_LOAD) {
                        callback.accept(level, chunk);
                    }
                });
        ServerLifecycleEvents.SERVER_STOPPING.register(
                s -> {
                    for (Consumer<MinecraftServer> callback : STOPPING) {
                        callback.accept(s);
                    }
                });
        ServerPlayerEvents.JOIN.register(
                player -> {
                    for (Consumer<ServerPlayer> callback : JOIN) {
                        callback.accept(player);
                    }
                });
        ServerPlayerEvents.LEAVE.register(
                player -> {
                    for (Consumer<ServerPlayer> callback : DISCONNECT) {
                        callback.accept(player);
                    }
                });
        ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, RESTORED_PHASE);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                RESTORED_PHASE,
                (oldPlayer, newPlayer, alive) -> {
                    for (Consumer<ServerPlayer> callback : RESPAWN) {
                        callback.accept(newPlayer);
                    }
                });
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> {
                    for (PlayerLevelChange callback : LEVEL_CHANGE) {
                        callback.accept(player, origin, destination);
                    }
                });
    }

    public static @Nullable MinecraftServer getCurrentServer() {
        return server;
    }

    public static void dispatchChunkUnload(ServerLevel level, LevelChunk chunk) {
        for (BiConsumer<ServerLevel, LevelChunk> callback : CHUNK_UNLOAD) {
            callback.accept(level, chunk);
        }
    }
}
