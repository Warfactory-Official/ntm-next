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

import com.hbm.NuclearTech;
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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class Network {
    public static final List<Registration<?>> clientbound = new ArrayList<>();
    public static final List<Registration<?>> serverbound = new ArrayList<>();
    public static final List<PreJoinSync<?>> preJoin = new ArrayList<>();
    public static final List<DatapackSync<?>> datapackSync = new ArrayList<>();

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
        ServerLevel level = (ServerLevel) entity.level();
        return level.getChunkSource().chunkMap.getPlayersWatching(entity);
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
        PayloadRegistrar registrar = event.registrar(NuclearTech.MOD_ID);
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

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> IPayloadHandler<T> directHandler(
            BiConsumer<T, PayloadHandlerContext> handler, PayloadHandlerThread thread) {
        BiConsumer<EncodedPayload, PayloadHandlerContext> receiver =
                (payload, context) -> handler.accept((T) payload, context);
        return (payload, context) -> {
            EncodedPayload encoded = (EncodedPayload) payload;
            encoded.prepareReceive(context, receiver);
            var processor = context.listener().getPacketProcessor();
            if (thread == PayloadHandlerThread.NETWORK || processor.isSameThread()) {
                encoded.run();
            } else {
                processor.scheduleIfPossible(encoded);
            }
        };
    }

    public record Registration<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, PayloadHandlerContext> handler,
            PayloadHandlerThread thread) {
        public void registerClientbound(PayloadRegistrar registrar) {
            if (codec instanceof EncodedPayload.Codec) {
                registrar
                        .executesOn(HandlerThread.NETWORK)
                        .playToClient(type, codec, directHandler());
            } else {
                registrar
                        .executesOn(
                                thread == PayloadHandlerThread.NETWORK
                                        ? HandlerThread.NETWORK
                                        : HandlerThread.MAIN)
                        .playToClient(
                                type,
                                codec,
                                (payload, ctx) -> handler.accept(payload, ctx::player));
            }
        }

        public void registerServerbound(PayloadRegistrar registrar) {
            if (codec instanceof EncodedPayload.Codec) {
                registrar
                        .executesOn(HandlerThread.NETWORK)
                        .playToServer(type, codec, directHandler());
            } else {
                registrar
                        .executesOn(
                                thread == PayloadHandlerThread.NETWORK
                                        ? HandlerThread.NETWORK
                                        : HandlerThread.MAIN)
                        .playToServer(
                                type,
                                codec,
                                (payload, ctx) -> handler.accept(payload, ctx::player));
            }
        }

        private IPayloadHandler<T> directHandler() {
            return Network.directHandler(handler, thread);
        }
    }

    public record DatapackSync<T extends CustomPacketPayload>(
            Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            Consumer<T> apply,
            Function<MinecraftServer, T> snapshot) {
        public void register(PayloadRegistrar registrar) {
            if (EncodedPayload.Codec.class.isInstance(codec)) {
                registrar
                        .executesOn(HandlerThread.NETWORK)
                        .playToClient(type, codec, directHandler());
            } else {
                registrar
                        .executesOn(HandlerThread.MAIN)
                        .playToClient(type, codec, (payload, ctx) -> apply.accept(payload));
            }
        }

        private IPayloadHandler<T> directHandler() {
            return Network.directHandler(
                    (payload, context) -> apply.accept(payload), PayloadHandlerThread.MAIN);
        }

        public void send(MinecraftServer server, List<ServerPlayer> players) {
            T payload = snapshot.apply(server);
            try {
                if (payload instanceof EncodedPayload encoded) encoded.prepareManaged();
                for (ServerPlayer player : players) {
                    if (player.connection.hasChannel(type))
                        PacketDistributor.sendToPlayer(player, payload);
                }
            } finally {
                if (payload instanceof EncodedPayload encoded) encoded.release();
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
                    T payload = snapshot.get();
                    try {
                        if (payload instanceof EncodedPayload encoded) encoded.prepareManaged();
                        send.accept(payload);
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
