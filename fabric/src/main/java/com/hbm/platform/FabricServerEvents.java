// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class FabricServerEvents {
    public static final Identifier RESTORED_PHASE = Library.id("restored");
    public static final List<BiConsumer<ServerLevel, LevelChunk>> CHUNK_UNLOAD = new ArrayList<>();
    public static final List<Consumer<MinecraftServer>> TICK_PRE = new ArrayList<>();
    public static final List<Consumer<MinecraftServer>> TICK_POST = new ArrayList<>();
    public static final List<BiConsumer<ServerLevel, LevelChunk>> CHUNK_LOAD = new ArrayList<>();
    public static final List<Consumer<MinecraftServer>> STOPPING = new ArrayList<>();
    public static final List<Consumer<ServerPlayer>> JOIN = new ArrayList<>();
    public static final List<Consumer<ServerPlayer>> DISCONNECT = new ArrayList<>();
    public static final List<Consumer<ServerPlayer>> RESPAWN = new ArrayList<>();
    public static final List<PlayerLevelChange> LEVEL_CHANGE = new ArrayList<>();
    public static volatile @Nullable MinecraftServer server;

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

    public static void dispatchChunkUnload(ServerLevel level, LevelChunk chunk) {
        for (BiConsumer<ServerLevel, LevelChunk> callback : CHUNK_UNLOAD) {
            callback.accept(level, chunk);
        }
    }

    public static @Nullable MinecraftServer getCurrentServer() {
        return server;
    }

    public static void onStartTracking(BiConsumer<Entity, ServerPlayer> callback) {
        EntityTrackingEvents.START_TRACKING.register(callback::accept);
    }

    public static void onEntityLoad(BiConsumer<Entity, ServerLevel> callback) {
        ServerEntityEvents.ENTITY_LOAD.register(callback::accept);
    }

    public static void onEntityUnload(BiConsumer<Entity, ServerLevel> callback) {
        ServerEntityEvents.ENTITY_UNLOAD.register(callback::accept);
    }

    public static void onServerTickPre(Consumer<MinecraftServer> callback) {
        TICK_PRE.add(callback);
    }

    public static void onServerTickPost(Consumer<MinecraftServer> callback) {
        TICK_POST.add(callback);
    }

    public static void onLevelLoad(Consumer<ServerLevel> callback) {
        ServerLevelEvents.LOAD.register((server, level) -> callback.accept(level));
    }

    public static void onChunkLoad(BiConsumer<ServerLevel, LevelChunk> callback) {
        CHUNK_LOAD.add(callback);
    }

    public static void onChunkUnload(BiConsumer<ServerLevel, LevelChunk> callback) {
        CHUNK_UNLOAD.add(callback);
    }

    public static void onServerStopping(Consumer<MinecraftServer> callback) {
        STOPPING.add(callback);
    }

    public static void onPlayerJoin(Consumer<ServerPlayer> callback) {
        JOIN.add(callback);
    }

    public static void onPlayerDisconnect(Consumer<ServerPlayer> callback) {
        DISCONNECT.add(callback);
    }

    public static void onPlayerRespawn(Consumer<ServerPlayer> callback) {
        RESPAWN.add(callback);
    }

    public static void onPlayerChangeLevel(PlayerLevelChange callback) {
        LEVEL_CHANGE.add(callback);
    }
}
