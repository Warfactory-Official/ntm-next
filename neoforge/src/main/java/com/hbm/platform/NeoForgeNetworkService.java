// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.NuclearTech;
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
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeNetworkService implements INetworkService {

    private static final String VERSION = "1.0.0";

    private final List<Registration<?>> clientbound = new ArrayList<>();
    private final List<Registration<?>> serverbound = new ArrayList<>();
    private final List<PreJoinSync<?>> preJoin = new ArrayList<>();
    private final List<DatapackSync<?>> datapackSync = new ArrayList<>();

    private static <T extends CustomPacketPayload> void registerClientbound0(
            PayloadRegistrar registrar, Registration<T> r) {
        registrar
                .executesOn(map(r.thread))
                .playToClient(
                        r.type, r.codec, (payload, ctx) -> r.handler.accept(payload, ctx::player));
    }

    private static <T extends CustomPacketPayload> void registerServerbound0(
            PayloadRegistrar registrar, Registration<T> r) {
        registrar
                .executesOn(map(r.thread))
                .playToServer(
                        r.type, r.codec, (payload, ctx) -> r.handler.accept(payload, ctx::player));
    }

    private static HandlerThread map(PayloadHandlerThread thread) {
        return thread == PayloadHandlerThread.NETWORK ? HandlerThread.NETWORK : HandlerThread.MAIN;
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

    private static <T extends CustomPacketPayload> void registerPreJoin0(
            PayloadRegistrar registrar, PreJoinSync<T> s) {

        registrar
                .executesOn(HandlerThread.NETWORK)
                .configurationToClient(s.type, s.codec, (payload, ctx) -> s.apply.accept(payload));
    }

    @Override
    public <T extends CustomPacketPayload> void registerDatapackSync(
            Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            Consumer<T> apply,
            Function<MinecraftServer, T> snapshot) {
        datapackSync.add(new DatapackSync<>(type, codec, apply, snapshot));
    }

    private static <T extends CustomPacketPayload> void registerDatapackSync0(
            PayloadRegistrar registrar, DatapackSync<T> s) {
        registrar
                .executesOn(HandlerThread.MAIN)
                .playToClient(s.type, s.codec, (payload, ctx) -> s.apply.accept(payload));
    }

    public void onDatapackSync(OnDatapackSyncEvent event) {
        List<ServerPlayer> remote =
                event.getRelevantPlayers()
                        .filter(player -> !player.connection.getConnection().isMemoryConnection())
                        .toList();
        if (remote.isEmpty()) return;
        for (DatapackSync<?> s : datapackSync)
            sendDatapackSync(s, event.getPlayerList().getServer(), remote);
    }

    private static <T extends CustomPacketPayload> void sendDatapackSync(
            DatapackSync<T> s, MinecraftServer server, List<ServerPlayer> players) {
        T payload = s.snapshot.apply(server);
        for (ServerPlayer player : players) {
            if (player.connection.hasChannel(s.type))
                PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public void flush(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NuclearTech.MOD_ID).versioned(VERSION);
        for (Registration<?> r : clientbound) registerClientbound0(registrar, r);
        for (Registration<?> r : serverbound) registerServerbound0(registrar, r);
        for (PreJoinSync<?> s : preJoin) registerPreJoin0(registrar, s);
        for (DatapackSync<?> s : datapackSync) registerDatapackSync0(registrar, s);
    }

    public void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        ServerConfigurationPacketListener listener = event.getListener();

        if (listener.getConnection().isMemoryConnection()) return;
        for (PreJoinSync<?> s : preJoin) {
            if (listener.hasChannel(s.type)) event.register(s.task());
        }
    }

    @Override
    public Channel channelOf(ServerPlayer player) {
        return player.connection.getConnection().channel();
    }

    @Override
    public Connection connectionOf(ServerPlayer player) {
        return player.connection.getConnection();
    }

    @Override
    public void validateClientboundPayload(
            ServerPlayer player, ClientboundCustomPayloadPacket packet) {
        NetworkRegistry.checkPacket(packet, player.connection);
    }

    @Override
    public Collection<ServerPlayer> playersWatchingEntity(Entity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        return level.getChunkSource().chunkMap.getPlayersWatching(entity);
    }

    private record Registration<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, IPayloadHandlerContext> handler,
            PayloadHandlerThread thread) {}

    private record DatapackSync<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            Consumer<T> apply,
            Function<MinecraftServer, T> snapshot) {}

    private record PreJoinSync<T extends CustomPacketPayload>(
            Type<T> type, StreamCodec<ByteBuf, T> codec, Consumer<T> apply, Supplier<T> snapshot) {

        ICustomConfigurationTask task() {
            ConfigurationTask.Type key = new ConfigurationTask.Type(type.id());
            return new ICustomConfigurationTask() {
                @Override
                public void run(Consumer<CustomPacketPayload> send) {
                    send.accept(snapshot.get());
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
