// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.PayloadHandlerThread;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.world.entity.player.Player;

public final class FabricNetwork {
    public static final int DATAPACK_SYNC_MAX_BYTES = 64 * 1024 * 1024;

    public static final List<Registration<?>> clientbound = new ArrayList<>();
    public static final List<Registration<?>> serverbound = new ArrayList<>();
    public static final List<PreJoinSync<?>> preJoin = new ArrayList<>();
    public static final List<DatapackSync<?>> datapackSync = new ArrayList<>();
    public static boolean flushed;

    public static IPayloadHandlerContext wrap(Player player) {
        return () -> player;
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

    public static void flush() {
        if (flushed) return;
        flushed = true;
        for (Registration<?> r : clientbound) r.registerClientboundType();
        for (Registration<?> r : serverbound) r.registerServerbound();
        for (PreJoinSync<?> s : preJoin) s.registerType();
        for (DatapackSync<?> s : datapackSync) s.registerType();
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(
                (player, joined) -> {
                    if (player.connection.connection.isMemoryConnection()) return;
                    for (DatapackSync<?> s : datapackSync) s.send(player);
                });
        ServerConfigurationConnectionEvents.CONFIGURE.register(
                (listener, server) -> {
                    if (listener.connection.isMemoryConnection()) return;
                    for (PreJoinSync<?> sync : preJoin) {
                        if (ServerConfigurationNetworking.canSend(listener, sync.type()))
                            listener.addTask(sync.task());
                    }
                });
    }

    public static void validateClientboundPayload(
            ServerPlayer player, ClientboundCustomPayloadPacket packet) {}

    public record Registration<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, IPayloadHandlerContext> handler,
            PayloadHandlerThread thread) {
        public void registerClientboundType() {
            PayloadTypeRegistry.clientboundPlay().register(type, codec);
        }

        public void registerServerbound() {
            PayloadTypeRegistry.serverboundPlay().register(type, codec);
            ServerPlayNetworking.registerGlobalReceiver(
                    type, (payload, ctx) -> handler.accept(payload, wrap(ctx.player())));
        }
    }

    public record DatapackSync<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            Consumer<T> apply,
            Function<MinecraftServer, T> snapshot) {
        public void registerType() {
            PayloadTypeRegistry.clientboundPlay()
                    .registerLarge(type, codec, DATAPACK_SYNC_MAX_BYTES);
        }

        public void send(ServerPlayer player) {
            if (ServerPlayNetworking.canSend(player, type)) {
                ServerPlayNetworking.send(player, snapshot.apply(player.level().getServer()));
            }
        }
    }

    public record PreJoinSync<T extends CustomPacketPayload>(
            Type<T> type, StreamCodec<ByteBuf, T> codec, Consumer<T> apply, Supplier<T> snapshot) {
        public void registerType() {
            PayloadTypeRegistry.clientboundConfiguration().register(type, codec);
        }

        public ConfigurationTask task() {
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
