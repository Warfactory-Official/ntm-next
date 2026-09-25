// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.PayloadHandlerThread;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class FabricNetworkService implements INetworkService {

    private static final int DATAPACK_SYNC_MAX_BYTES = 64 * 1024 * 1024;

    private final List<Registration<?>> clientbound = new ArrayList<>();
    private final List<Registration<?>> serverbound = new ArrayList<>();
    private final List<PreJoinSync<?>> preJoin = new ArrayList<>();
    private final List<DatapackSync<?>> datapackSync = new ArrayList<>();
    private boolean flushed;

    private static <T extends CustomPacketPayload> void registerClientboundType(Registration<T> r) {
        PayloadTypeRegistry.clientboundPlay().register(r.type(), r.codec());
    }

    private static <T extends CustomPacketPayload> void registerPreJoinType(PreJoinSync<T> s) {
        PayloadTypeRegistry.clientboundConfiguration().register(s.type(), s.codec());
    }

    private static <T extends CustomPacketPayload> void registerServerbound0(Registration<T> r) {
        PayloadTypeRegistry.serverboundPlay().register(r.type(), r.codec());
        ServerPlayNetworking.registerGlobalReceiver(
                r.type(), (payload, ctx) -> r.handler().accept(payload, wrap(ctx.player())));
    }

    public static IPayloadHandlerContext wrap(Player player) {
        return () -> player;
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(
            Type<T> type,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, IPayloadHandlerContext> handler,
            PayloadHandlerThread thread) {
        clientbound.add(new Registration<>(type, codec, handler, thread));
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerbound(
            Type<T> type,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, IPayloadHandlerContext> handler,
            PayloadHandlerThread thread) {
        serverbound.add(new Registration<>(type, codec, handler, thread));
    }

    @Override
    public <T extends CustomPacketPayload> void registerPreJoinSync(
            Type<T> type, StreamCodec<ByteBuf, T> codec, Consumer<T> apply, Supplier<T> snapshot) {
        preJoin.add(new PreJoinSync<>(type, codec, apply, snapshot));
        clientbound.add(
                new Registration<>(
                        type,
                        codec,
                        (payload, ctx) -> apply.accept(payload),
                        PayloadHandlerThread.MAIN));
    }

    @Override
    public <T extends CustomPacketPayload> void registerDatapackSync(
            Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            Consumer<T> apply,
            Function<MinecraftServer, T> snapshot) {
        datapackSync.add(new DatapackSync<>(type, codec, apply, snapshot));
    }

    private static <T extends CustomPacketPayload> void registerDatapackSyncType(
            DatapackSync<T> s) {
        PayloadTypeRegistry.clientboundPlay()
                .registerLarge(s.type(), s.codec(), DATAPACK_SYNC_MAX_BYTES);
    }

    private static <T extends CustomPacketPayload> void sendDatapackSync(
            DatapackSync<T> s, ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, s.type())) {
            ServerPlayNetworking.send(player, s.snapshot().apply(player.level().getServer()));
        }
    }

    public void flush() {
        if (flushed) return;
        flushed = true;
        for (Registration<?> r : clientbound) registerClientboundType(r);
        for (Registration<?> r : serverbound) registerServerbound0(r);
        for (PreJoinSync<?> s : preJoin) registerPreJoinType(s);
        for (DatapackSync<?> s : datapackSync) registerDatapackSyncType(s);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(
                (player, joined) -> {
                    if (player.connection.connection.isMemoryConnection()) return;
                    for (DatapackSync<?> s : datapackSync) sendDatapackSync(s, player);
                });

        ServerConfigurationConnectionEvents.CONFIGURE.register(
                (listener, server) -> {
                    if (sharesTheServerMaps(listener)) return;
                    for (PreJoinSync<?> sync : preJoin) {
                        if (ServerConfigurationNetworking.canSend(listener, sync.type()))
                            listener.addTask(sync.task());
                    }
                });
    }

    private static boolean sharesTheServerMaps(ServerConfigurationPacketListenerImpl listener) {
        return listener.connection.isMemoryConnection();
    }

    public List<Registration<?>> clientboundRegistrations() {
        return clientbound;
    }

    public List<PreJoinSync<?>> preJoinSyncs() {
        return preJoin;
    }

    public List<DatapackSync<?>> datapackSyncs() {
        return datapackSync;
    }

    @Override
    public Channel channelOf(ServerPlayer player) {
        return player.connection.connection.channel;
    }

    @Override
    public Connection connectionOf(ServerPlayer player) {
        return player.connection.connection;
    }

    @Override
    public Collection<ServerPlayer> playersWatchingEntity(Entity entity) {
        return PlayerLookup.tracking(entity);
    }

    public record Registration<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, IPayloadHandlerContext> handler,
            PayloadHandlerThread thread) {}

    public record DatapackSync<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            Consumer<T> apply,
            Function<MinecraftServer, T> snapshot) {}

    public record PreJoinSync<T extends CustomPacketPayload>(
            Type<T> type, StreamCodec<ByteBuf, T> codec, Consumer<T> apply, Supplier<T> snapshot) {

        ConfigurationTask task() {
            ConfigurationTask.Type key = new ConfigurationTask.Type(type.id().toString());
            return new ConfigurationTask() {
                @Override
                public void start(Consumer<Packet<?>> send) {
                    send.accept(
                            ServerConfigurationNetworking.createClientboundPacket(snapshot.get()));
                }

                @Override
                public boolean tick() {
                    return true;
                }

                @Override
                public ConfigurationTask.Type type() {
                    return key;
                }
            };
        }
    }
}
