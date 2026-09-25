// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.platform.services.IServerAccessor;
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
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

public final class NeoForgeServerAccessor implements IServerAccessor {

    private final List<Consumer<MinecraftServer>> tickPreCallbacks = new ArrayList<>();
    private final List<Consumer<MinecraftServer>> tickCallbacks = new ArrayList<>();
    private final List<BiConsumer<ServerLevel, LevelChunk>> chunkLoadCallbacks = new ArrayList<>();
    private final List<BiConsumer<ServerLevel, LevelChunk>> chunkUnloadCallbacks =
            new ArrayList<>();
    private final List<Consumer<MinecraftServer>> stoppingCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> joinCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> disconnectCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> respawnCallbacks = new ArrayList<>();
    private final List<PlayerLevelChange> levelChangeCallbacks = new ArrayList<>();

    public NeoForgeServerAccessor() {

        NeoForge.EVENT_BUS.addListener(
                (ServerTickEvent.Pre event) -> {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        for (Consumer<MinecraftServer> callback : tickPreCallbacks) {
                            callback.accept(server);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ServerTickEvent.Post event) -> {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        for (Consumer<MinecraftServer> callback : tickCallbacks) {
                            callback.accept(server);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ChunkEvent.Load event) -> {
                    if (event.getLevel() instanceof ServerLevel level
                            && event.getChunk() instanceof LevelChunk chunk) {
                        for (BiConsumer<ServerLevel, LevelChunk> callback : chunkLoadCallbacks) {
                            callback.accept(level, chunk);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ChunkEvent.Unload event) -> {
                    if (event.getLevel() instanceof ServerLevel level
                            && event.getChunk() instanceof LevelChunk chunk) {
                        for (BiConsumer<ServerLevel, LevelChunk> callback : chunkUnloadCallbacks) {
                            callback.accept(level, chunk);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerLoggedInEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        for (Consumer<ServerPlayer> callback : joinCallbacks) {
                            callback.accept(player);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerLoggedOutEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        for (Consumer<ServerPlayer> callback : disconnectCallbacks) {
                            callback.accept(player);
                        }
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerRespawnEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        for (Consumer<ServerPlayer> callback : respawnCallbacks) {
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
                    for (PlayerLevelChange callback : levelChangeCallbacks) {
                        callback.accept(player, origin, destination);
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (ServerStoppingEvent event) -> {
                    for (Consumer<MinecraftServer> callback : stoppingCallbacks) {
                        callback.accept(event.getServer());
                    }
                });
    }

    @Override
    public @Nullable MinecraftServer getCurrentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public void onStartTracking(BiConsumer<Entity, ServerPlayer> callback) {
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.StartTracking event) ->
                        callback.accept(event.getTarget(), (ServerPlayer) event.getEntity()));
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
        NeoForge.EVENT_BUS.addListener(
                (LevelEvent.Load event) -> {
                    if (event.getLevel() instanceof ServerLevel level) callback.accept(level);
                });
    }

    @Override
    public void onChunkLoad(BiConsumer<ServerLevel, LevelChunk> callback) {
        chunkLoadCallbacks.add(callback);
    }

    @Override
    public void onChunkUnload(BiConsumer<ServerLevel, LevelChunk> callback) {
        chunkUnloadCallbacks.add(callback);
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

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        stoppingCallbacks.add(callback);
    }
}
