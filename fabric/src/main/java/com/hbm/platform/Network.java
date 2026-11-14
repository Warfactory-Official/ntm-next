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

import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.PayloadHandlerThread;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class Network {
    public static final List<Registration<?>> clientbound = new ArrayList<>();
    public static final List<Registration<?>> serverbound = new ArrayList<>();
    public static final List<PreJoinSync<?>> preJoin = new ArrayList<>();
    public static final List<DatapackSync<?>> datapackSync = new ArrayList<>();
    private static final int DATAPACK_SYNC_MAX_BYTES = EncodedPayload.MAX_DATAPACK_BYTES;
    private static boolean flushed;

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

    public static Collection<ServerPlayer> playersWatchingEntity(Entity entity) {
        return PlayerLookup.tracking(entity);
    }

    public static PayloadHandlerContext context(CustomPacketPayload payload, Player player) {
        return payload instanceof EncodedPayload encoded
                ? encoded.context(player)
                : ((PayloadHandlerContext) (() -> player));
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

    public record Registration<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, PayloadHandlerContext> handler,
            PayloadHandlerThread thread) {
        public Registration {
            if (thread == PayloadHandlerThread.NETWORK
                    && codec instanceof EncodedPayload.Codec direct) direct.networkHandler(handler);
        }

        public void registerClientboundType() {
            PayloadTypeRegistry.clientboundPlay().register(type, codec);
        }

        public void registerServerbound() {
            PayloadTypeRegistry.serverboundPlay().register(type, codec);
            ServerPlayNetworking.registerGlobalReceiver(
                    type,
                    (payload, ctx) -> handler.accept(payload, context(payload, ctx.player())));
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
                T payload = snapshot.apply(player.level().getServer());
                try {
                    if (payload instanceof EncodedPayload encoded) encoded.prepareManaged();
                    ServerPlayNetworking.send(player, payload);
                } finally {
                    if (payload instanceof EncodedPayload encoded) encoded.release();
                }
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
                    T payload = snapshot.get();
                    try {
                        if (payload instanceof EncodedPayload encoded) encoded.prepareManaged();
                        send.accept(ServerConfigurationNetworking.createClientboundPacket(payload));
                    } finally {
                        if (payload instanceof EncodedPayload encoded) encoded.release();
                    }
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
