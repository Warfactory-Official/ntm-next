// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.PacketWire;
import com.hbm.packet.PayloadHandlerThread;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.util.Collection;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public interface INetworkService {

    <T extends CustomPacketPayload> void registerClientbound(
            Type<T> type,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, IPayloadHandlerContext> handler,
            PayloadHandlerThread thread);

    <T extends CustomPacketPayload> void registerServerbound(
            Type<T> type,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, IPayloadHandlerContext> handler,
            PayloadHandlerThread thread);

    <T extends CustomPacketPayload> void registerPreJoinSync(
            Type<T> type, StreamCodec<ByteBuf, T> codec, Consumer<T> apply, Supplier<T> snapshot);

    <T extends CustomPacketPayload> void registerDatapackSync(
            Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            Consumer<T> apply,
            Function<MinecraftServer, T> snapshot);

    Channel channelOf(ServerPlayer player);

    Connection connectionOf(ServerPlayer player);

    default void validateClientboundPayload(
            ServerPlayer player, ClientboundCustomPayloadPacket packet) {}

    Collection<ServerPlayer> playersWatchingEntity(Entity entity);

    default void sendToDimension(ThreadedPayload payload, ServerLevel level) {
        PacketWire.sendToDimension(payload, level);
    }

    default void sendToAllAround(ThreadedPayload payload, TargetPoint target) {
        PacketWire.sendToAllAround(payload, target);
    }

    default void sendToAllTracking(ThreadedPayload payload, TargetPoint point) {
        PacketWire.sendToAllTracking(payload, point);
    }

    default void sendToAllTracking(ThreadedPayload payload, Entity entity) {
        PacketWire.sendToAllTracking(payload, entity);
    }

    default void sendTo(ThreadedPayload payload, ServerPlayer player) {
        PacketWire.sendTo(payload, player);
    }

    default void sendToAll(ThreadedPayload payload) {
        PacketWire.sendToAll(payload);
    }

    default void sendToServer(ThreadedPayload payload) {
        PacketWire.sendToServer(payload);
    }

    default void sendToPlayers(ThreadedPayload payload, Collection<ServerPlayer> players) {
        PacketWire.sendToPlayers(payload, players);
    }
}
