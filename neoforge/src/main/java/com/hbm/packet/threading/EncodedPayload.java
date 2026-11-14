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

package com.hbm.packet.threading;

import com.hbm.NuclearTech;
import com.hbm.packet.FarAudience;
import com.hbm.packet.PacketWire;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketBuffers;
import com.hbm.packet.compat.PayloadTypeNames;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class EncodedPayload extends ThreadedPayload
        implements Runnable, AutoCloseable, PayloadHandlerContext {
    public static final int MAX_SERVERBOUND_BYTES = 32767;
    public static final int MAX_CLIENTBOUND_BYTES = 1048576;
    public static final int MAX_DATAPACK_BYTES = 64 * 1024 * 1024;
    private static final ConcurrentHashMap<
                    Type<EncodedPayload>, Function<ByteBuf, ClientboundBlockEntityDataPacket>>
            RECORDERS = new ConcurrentHashMap<>();

    private final Type<EncodedPayload> type;
    private boolean incoming;
    private volatile boolean queued;
    private BiConsumer<EncodedPayload, PayloadHandlerContext> receiveHandler;
    private Object receiveContext;
    private PacketListener receiveListener;
    private boolean networkThread;

    private EncodedPayload(
            Type<EncodedPayload> type, ByteBuf buffer, int prefixLength, boolean incoming) {
        super(buffer, prefixLength);
        this.type = type;
        this.incoming = incoming;
    }

    public static EncodedPayload allocate(
            Type<EncodedPayload> type, int bodyBytes, boolean clientbound) {
        return allocate(type, bodyBytes, bodyBytes, clientbound, false, false);
    }

    public static EncodedPayload allocate(
            Type<EncodedPayload> type,
            int initialBodyBytes,
            int maxBodyBytes,
            boolean clientbound) {
        return allocate(type, initialBodyBytes, maxBodyBytes, clientbound, false, false);
    }

    public static EncodedPayload allocateManaged(
            Type<EncodedPayload> type,
            int initialBodyBytes,
            int maxBodyBytes,
            boolean clientbound,
            boolean registryAware) {
        ByteBuf buffer = Unpooled.buffer(initialBodyBytes, maxBodyBytes);
        try {
            if (registryAware) buffer = PacketBuffers.registry(buffer, clientbound);
            return new EncodedPayload(type, buffer, 0, false);
        } catch (Throwable failure) {
            buffer.release();
            throw failure;
        }
    }

    public static EncodedPayload allocate(
            Type<EncodedPayload> type,
            int initialBodyBytes,
            int maxBodyBytes,
            boolean clientbound,
            boolean farAudience,
            boolean registryAware) {
        byte[] packetId = clientbound ? PacketWire.ccpPacketIdBytes() : null;
        byte[] identifier = packetId == null ? null : PayloadTypeNames.wire(type);
        int prefixLength = packetId == null ? 0 : packetId.length + identifier.length;
        int maxCapacity = (int) Math.min(Integer.MAX_VALUE, (long) prefixLength + maxBodyBytes);
        ByteBuf buffer =
                !clientbound || PacketWire.managedTransport()
                        ? Unpooled.buffer(prefixLength + initialBodyBytes, maxCapacity)
                        : PooledByteBufAllocator.DEFAULT.directBuffer(
                                prefixLength + initialBodyBytes, maxCapacity);
        try {
            if (packetId != null) {
                buffer.writeBytes(packetId);
                buffer.writeBytes(identifier);
            }
            if (registryAware) buffer = PacketBuffers.registry(buffer, clientbound);
            return farAudience
                    ? new FarEncodedPayload(type, buffer, prefixLength)
                    : new EncodedPayload(type, buffer, prefixLength, false);
        } catch (Throwable failure) {
            buffer.release();
            throw failure;
        }
    }

    public static StreamCodec<ByteBuf, EncodedPayload> codec(
            Type<EncodedPayload> type, int bodyBytes) {
        return new Codec(type, bodyBytes, bodyBytes, false);
    }

    public static StreamCodec<ByteBuf, EncodedPayload> variableCodec(
            Type<EncodedPayload> type, int minBodyBytes, int maxBodyBytes) {
        return variableCodec(type, minBodyBytes, maxBodyBytes, false);
    }

    public static StreamCodec<ByteBuf, EncodedPayload> variableCodec(
            Type<EncodedPayload> type, int minBodyBytes, int maxBodyBytes, boolean registryAware) {
        return new Codec(type, minBodyBytes, maxBodyBytes, registryAware);
    }

    public static final class Codec implements StreamCodec<ByteBuf, EncodedPayload> {
        private final Type<EncodedPayload> type;
        private final int minBodyBytes;
        private final int maxBodyBytes;
        private final boolean registryAware;
        private volatile BiConsumer<EncodedPayload, PayloadHandlerContext> networkReceiver;

        private Codec(
                Type<EncodedPayload> type,
                int minBodyBytes,
                int maxBodyBytes,
                boolean registryAware) {
            this.type = type;
            this.minBodyBytes = minBodyBytes;
            this.maxBodyBytes = maxBodyBytes;
            this.registryAware = registryAware;
        }

        @SuppressWarnings("unchecked")
        public <T extends CustomPacketPayload> void networkHandler(
                BiConsumer<T, PayloadHandlerContext> handler) {
            networkReceiver = (payload, context) -> handler.accept((T) payload, context);
        }

        public void recording(Function<ByteBuf, ClientboundBlockEntityDataPacket> recorder) {
            RECORDERS.put(type, recorder);
        }

        @Override
        public EncodedPayload decode(ByteBuf inputBuffer) {
            int length = inputBuffer.readableBytes();
            if (length < minBodyBytes || length > maxBodyBytes)
                throw new DecoderException("Invalid " + type.id() + " length");
            ByteBuf body = inputBuffer.readRetainedSlice(length);
            try {
                if (registryAware && inputBuffer instanceof RegistryFriendlyByteBuf registry) {
                    body = new RegistryFriendlyByteBuf(body, registry.registryAccess());
                }
                EncodedPayload payload = new EncodedPayload(type, body, 0, true);
                payload.receiveHandler = networkReceiver;
                payload.networkThread = payload.receiveHandler != null;
                return payload;
            } catch (Throwable failure) {
                body.release();
                throw failure;
            }
        }

        @Override
        public void encode(ByteBuf outputBuffer, EncodedPayload payload) {
            payload.writeManagedBody(outputBuffer);
        }
    }

    private static final class FarEncodedPayload extends EncodedPayload implements FarAudience {
        private FarEncodedPayload(Type<EncodedPayload> type, ByteBuf buffer, int prefixLength) {
            super(type, buffer, prefixLength, false);
        }
    }

    public void prepareReceive(
            Player player,
            BiConsumer<EncodedPayload, PayloadHandlerContext> handler,
            PacketListener listener) {
        receiveContext = player;
        receiveHandler = handler;
        receiveListener = listener;
    }

    public void prepareReceive(
            IPayloadContext context, BiConsumer<EncodedPayload, PayloadHandlerContext> handler) {
        receiveContext = context;
        receiveHandler = handler;
        receiveListener = context.listener();
    }

    public PayloadHandlerContext context(Player player) {
        receiveContext = player;
        return this;
    }

    @Override
    public Player player() {
        if (receiveContext instanceof IPayloadContext context) return context.player();
        return (Player) receiveContext;
    }

    public void markQueued() {
        queued = true;
    }

    @Override
    public void run() {
        try {
            if (receiveListener.isAcceptingMessages()) receiveHandler.accept(this, this);
        } catch (Exception failure) {
            NuclearTech.LOGGER.error("Failed to process incoming payload {}", type.id(), failure);
            ((ICommonPacketListener) receiveListener)
                    .disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
        } finally {
            close();
        }
    }

    @Override
    public synchronized void close() {
        if (incoming) {
            incoming = false;
            release();
        }
    }

    public static void markQueued(Packet<?> packet) {
        EncodedPayload payload = incoming(packet);
        if (payload != null) payload.markQueued();
    }

    public static boolean isEncoded(Packet<?> packet) {
        return incoming(packet) != null;
    }

    public static boolean isNetwork(Packet<?> packet) {
        EncodedPayload payload = incoming(packet);
        return payload != null && payload.networkThread;
    }

    public static void receiveNetwork(Packet<?> packet) {
        EncodedPayload payload = incoming(packet);
        try {
            payload.receiveHandler.accept(payload, null);
        } finally {
            payload.close();
        }
    }

    public static void discard(Packet<?> packet) {
        EncodedPayload payload = incoming(packet);
        if (payload != null) payload.close();
    }

    public static void discardIfUnqueued(Packet<?> packet) {
        EncodedPayload payload = incoming(packet);
        if (payload != null && !payload.queued) payload.close();
    }

    private static EncodedPayload incoming(Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket custom
                && custom.payload() instanceof EncodedPayload payload) return payload;
        if (packet instanceof ServerboundCustomPayloadPacket custom
                && custom.payload() instanceof EncodedPayload payload) return payload;
        return null;
    }

    public ByteBuf buffer() {
        return getCompiledBuffer();
    }

    public ByteBuf bodyBuffer() {
        return getCompiledBuffer();
    }

    public int bodyReaderIndex() {
        return wirePrefixLen;
    }

    public ClientboundBlockEntityDataPacket recordedPacket() {
        Function<ByteBuf, ClientboundBlockEntityDataPacket> recorder = RECORDERS.get(type);
        if (recorder == null) return null;
        ByteBuf inputBuffer = getCompiledBuffer().duplicate();
        inputBuffer.readerIndex(wirePrefixLen);
        return recorder.apply(inputBuffer);
    }

    @Override
    public void toBytes(ByteBuf outputBuffer) {
        ByteBuf buffer = getCompiledBuffer();
        outputBuffer.writeBytes(buffer, wirePrefixLen, buffer.writerIndex() - wirePrefixLen);
    }

    @Override
    public Type<EncodedPayload> type() {
        return type;
    }
}
