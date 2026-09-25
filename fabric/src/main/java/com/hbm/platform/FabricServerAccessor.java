// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.lib.Library;
import com.hbm.platform.services.IServerAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
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

public final class FabricServerAccessor implements IServerAccessor {

    private static final Identifier RESTORED_PHASE = Library.id("restored");
    private static final List<BiConsumer<ServerLevel, LevelChunk>> CHUNK_UNLOAD_CALLBACKS =
            new ArrayList<>();
    private final List<Consumer<MinecraftServer>> tickPreCallbacks = new ArrayList<>();
    private final List<Consumer<MinecraftServer>> tickCallbacks = new ArrayList<>();
    private final List<BiConsumer<ServerLevel, LevelChunk>> chunkLoadCallbacks = new ArrayList<>();
    private final List<Consumer<MinecraftServer>> stoppingCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> joinCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> disconnectCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> respawnCallbacks = new ArrayList<>();
    private final List<PlayerLevelChange> levelChangeCallbacks = new ArrayList<>();

    private volatile @Nullable MinecraftServer server;

    public FabricServerAccessor() {
        ServerLifecycleEvents.SERVER_STARTING.register(s -> this.server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> this.server = null);

        ServerTickEvents.START_SERVER_TICK.register(
                s -> {
                    for (Consumer<MinecraftServer> callback : tickPreCallbacks) {
                        callback.accept(s);
                    }
                });
        ServerTickEvents.END_SERVER_TICK.register(
                s -> {
                    for (Consumer<MinecraftServer> callback : tickCallbacks) {
                        callback.accept(s);
                    }
                });
        ServerChunkEvents.CHUNK_LOAD.register(
                (level, chunk, generated) -> {
                    for (BiConsumer<ServerLevel, LevelChunk> callback : chunkLoadCallbacks) {
                        callback.accept(level, chunk);
                    }
                });
        ServerLifecycleEvents.SERVER_STOPPING.register(
                s -> {
                    for (Consumer<MinecraftServer> callback : stoppingCallbacks) {
                        callback.accept(s);
                    }
                });
        ServerPlayerEvents.JOIN.register(
                player -> {
                    for (Consumer<ServerPlayer> callback : joinCallbacks) {
                        callback.accept(player);
                    }
                });
        ServerPlayerEvents.LEAVE.register(
                player -> {
                    for (Consumer<ServerPlayer> callback : disconnectCallbacks) {
                        callback.accept(player);
                    }
                });

        ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, RESTORED_PHASE);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                RESTORED_PHASE,
                (oldPlayer, newPlayer, alive) -> {
                    for (Consumer<ServerPlayer> callback : respawnCallbacks) {
                        callback.accept(newPlayer);
                    }
                });
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> {
                    for (PlayerLevelChange callback : levelChangeCallbacks) {
                        callback.accept(player, origin, destination);
                    }
                });
    }

    public static void dispatchChunkUnload(ServerLevel level, LevelChunk chunk) {
        for (BiConsumer<ServerLevel, LevelChunk> callback : CHUNK_UNLOAD_CALLBACKS) {
            callback.accept(level, chunk);
        }
    }

    @Override
    public @Nullable MinecraftServer getCurrentServer() {
        return server;
    }

    @Override
    public void onStartTracking(BiConsumer<Entity, ServerPlayer> callback) {
        EntityTrackingEvents.START_TRACKING.register(callback::accept);
    }

    @Override
    public void onServerTickPre(Consumer<MinecraftServer> callback) {
        tickPreCallbacks.add(callback);
    }

    @Override
    public void onServerTickPost(Consumer<MinecraftServer> callback) {
        tickCallbacks.add(callback);
    }

    @Override
    public void onLevelLoad(Consumer<ServerLevel> callback) {
        ServerLevelEvents.LOAD.register((server, level) -> callback.accept(level));
    }

    @Override
    public void onChunkLoad(BiConsumer<ServerLevel, LevelChunk> callback) {
        chunkLoadCallbacks.add(callback);
    }

    @Override
    public void onChunkUnload(BiConsumer<ServerLevel, LevelChunk> callback) {
        CHUNK_UNLOAD_CALLBACKS.add(callback);
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        stoppingCallbacks.add(callback);
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        joinCallbacks.add(callback);
    }

    @Override
    public void onPlayerDisconnect(Consumer<ServerPlayer> callback) {
        disconnectCallbacks.add(callback);
    }

    @Override
    public void onPlayerRespawn(Consumer<ServerPlayer> callback) {
        respawnCallbacks.add(callback);
    }

    @Override
    public void onPlayerChangeLevel(PlayerLevelChange callback) {
        levelChangeCallbacks.add(callback);
    }
}
