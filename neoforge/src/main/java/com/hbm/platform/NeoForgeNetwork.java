// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.NuclearTech;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.PayloadHandlerThread;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
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

public final class NeoForgeNetwork {
    public static final String VERSION = "1.0.0";

    public static final List<Registration<?>> clientbound = new ArrayList<>();
    public static final List<Registration<?>> serverbound = new ArrayList<>();
    public static final List<PreJoinSync<?>> preJoin = new ArrayList<>();
    public static final List<DatapackSync<?>> datapackSync = new ArrayList<>();

    public static HandlerThread map(PayloadHandlerThread thread) {
        return thread == PayloadHandlerThread.NETWORK ? HandlerThread.NETWORK : HandlerThread.MAIN;
    }

    public static <T extends CustomPacketPayload> void registerPreJoinSync(
            Type<T> type, StreamCodec<ByteBuf, T> codec, Consumer<T> apply, Supplier<T> snapshot) {
        preJoin.add(new PreJoinSync<>(type, codec, apply, snapshot));
        clientbound.add(
                new Registration<>(
                        type,
                        codec,
                        (payload, ctx) -> apply.accept(payload),
                        PayloadHandlerThread.MAIN));
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        List<ServerPlayer> remote =
                event.getRelevantPlayers()
                        .filter(player -> !player.connection.getConnection().isMemoryConnection())
                        .toList();
        if (remote.isEmpty()) return;
        for (DatapackSync<?> s : datapackSync) s.send(event.getPlayerList().getServer(), remote);
    }

    public static void flush(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NuclearTech.MOD_ID).versioned(VERSION);
        for (Registration<?> r : clientbound) r.registerClientbound(registrar);
        for (Registration<?> r : serverbound) r.registerServerbound(registrar);
        for (PreJoinSync<?> s : preJoin) s.register(registrar);
        for (DatapackSync<?> s : datapackSync) s.register(registrar);
    }

    public static void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        ServerConfigurationPacketListener listener = event.getListener();
        if (listener.getConnection().isMemoryConnection()) return;
        for (PreJoinSync<?> s : preJoin) {
            if (listener.hasChannel(s.type())) event.register(s.task());
        }
    }

    public static void validateClientboundPayload(
            ServerPlayer player, ClientboundCustomPayloadPacket packet) {
        NetworkRegistry.checkPacket(packet, player.connection);
    }

    public static Collection<ServerPlayer> playersWatchingEntity(Entity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        return level.getChunkSource().chunkMap.getPlayersWatching(entity);
    }

    public record Registration<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, IPayloadHandlerContext> handler,
            PayloadHandlerThread thread) {
        public void registerClientbound(PayloadRegistrar registrar) {
            registrar
                    .executesOn(map(thread))
                    .playToClient(
                            type, codec, (payload, ctx) -> handler.accept(payload, ctx::player));
        }

        public void registerServerbound(PayloadRegistrar registrar) {
            registrar
                    .executesOn(map(thread))
                    .playToServer(
                            type, codec, (payload, ctx) -> handler.accept(payload, ctx::player));
        }
    }

    public record DatapackSync<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            Consumer<T> apply,
            Function<MinecraftServer, T> snapshot) {
        public void register(PayloadRegistrar registrar) {
            registrar
                    .executesOn(HandlerThread.MAIN)
                    .playToClient(type, codec, (payload, ctx) -> apply.accept(payload));
        }

        public void send(MinecraftServer server, List<ServerPlayer> players) {
            T payload = snapshot.apply(server);
            for (ServerPlayer player : players) {
                if (player.connection.hasChannel(type))
                    PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public record PreJoinSync<T extends CustomPacketPayload>(
            Type<T> type, StreamCodec<ByteBuf, T> codec, Consumer<T> apply, Supplier<T> snapshot) {
        public void register(PayloadRegistrar registrar) {
            registrar
                    .executesOn(HandlerThread.NETWORK)
                    .configurationToClient(type, codec, (payload, ctx) -> apply.accept(payload));
        }

        public ICustomConfigurationTask task() {
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
