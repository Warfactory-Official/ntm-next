// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform.services;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public interface IServerAccessor {
    @Nullable MinecraftServer getCurrentServer();

    void onServerTickPre(Consumer<MinecraftServer> callback);

    void onServerTickPost(Consumer<MinecraftServer> callback);

    void onLevelLoad(Consumer<ServerLevel> callback);

    void onChunkLoad(BiConsumer<ServerLevel, LevelChunk> callback);

    void onChunkUnload(BiConsumer<ServerLevel, LevelChunk> callback);

    void onPlayerJoin(Consumer<ServerPlayer> callback);

    void onStartTracking(BiConsumer<Entity, ServerPlayer> callback);

    void onPlayerDisconnect(Consumer<ServerPlayer> callback);

    void onPlayerRespawn(Consumer<ServerPlayer> callback);

    void onPlayerChangeLevel(PlayerLevelChange callback);

    void onServerStopping(Consumer<MinecraftServer> callback);

    @FunctionalInterface
    interface PlayerLevelChange {
        void accept(ServerPlayer player, ServerLevel origin, ServerLevel destination);
    }
}
