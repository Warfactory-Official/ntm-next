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

package com.hbm.packet.layout;

import com.hbm.client.ClientPacketEvents;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;
import static net.minecraft.core.BlockPos.of;

public final class MarkerLayout {
    public static final int VARIANT = 28;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 18;
    public static final int MAX_BODY_BYTES = 1048576;

    private MarkerLayout() {}

    public static EncodedPayload encode(
            int color, int ttlMillis, double maxDistance, Map<BlockPos, Component> markers) {
        int markersCount = markers.size();
        if (markersCount > 2097151) throw new IllegalArgumentException("Invalid markers count");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyClientEvents.TYPE,
                        Math.max(
                                MIN_BODY_BYTES,
                                (int) ((176L + (long) markersCount * 64L + 7L) >>> 3)),
                        MAX_BODY_BYTES,
                        true,
                        false,
                        false);
        try {
            ByteBuf encodedBuffer = payload.buffer();
            int bufferStart = encodedBuffer.writerIndex();
            Object storageBase;
            long storageAddress;
            if (encodedBuffer.hasArray()) {
                storageBase = encodedBuffer.array();
                storageAddress = BA_BASE + encodedBuffer.arrayOffset() + bufferStart;
            } else {
                storageBase = null;
                storageAddress = encodedBuffer.memoryAddress() + bufferStart;
            }
            ByteBuf codecBuffer = encodedBuffer;
            {
                long colorBits = color;
                long ttlMillisBits = ttlMillis;
                long maxDistanceBits = doubleToRawLongBits(maxDistance);
                U.putLong(
                        storageBase,
                        storageAddress,
                        28L | ((colorBits & 0xFFFFFFFFL) << 5) | (ttlMillisBits << 37));
                U.putLong(
                        storageBase,
                        storageAddress + 8L,
                        ((ttlMillisBits >>> 27) & 31L) | (maxDistanceBits << 5));
                U.putByte(storageBase, storageAddress + 16L, (byte) ((maxDistanceBits >>> 59)));
            }
            long markersAddress =
                    PacketValues.putVarInt(storageBase, storageAddress + 17L, markersCount);
            for (var marker : markers.entrySet()) {
                {
                    long positionBits = marker.getKey().asLong();
                    U.putLong(storageBase, markersAddress, positionBits);
                }
                markersAddress += 8L;
            }
            encodedBuffer.writerIndex(bufferStart + (int) (markersAddress - storageAddress));
            for (var marker : markers.entrySet()) {
                ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(
                        codecBuffer, marker.getValue());
            }
            return payload;
        } catch (Throwable encodingFailure) {
            payload.release();
            throw encodingFailure;
        }
    }

    static void receiveFields(
            ByteBuf decodedBuffer,
            PayloadHandlerContext context,
            Object storageBase,
            long storageAddress,
            int bufferStart,
            int bodyBytes) {
        if (bodyBytes < MIN_BODY_BYTES) throw new DecoderException("Invalid packet length");
        ByteBuf codecBuffer = decodedBuffer;
        long bodyLimit = storageAddress + bodyBytes;
        long colorToTtlMillisWord = U.getLong(storageBase, storageAddress);
        long ttlMillisToMaxDistanceWord = U.getLong(storageBase, storageAddress + 8L);
        long maxDistanceWord = (U.getByte(storageBase, storageAddress + 16L) & 255L);
        int color = (int) (colorToTtlMillisWord >>> 5);
        int ttlMillis = (int) ((colorToTtlMillisWord >>> 37) | (ttlMillisToMaxDistanceWord << 27));
        double maxDistance =
                longBitsToDouble(((ttlMillisToMaxDistanceWord >>> 5) | (maxDistanceWord << 59)));
        if ((maxDistanceWord >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        int markersCount = PacketValues.getVarInt(storageBase, storageAddress + 17L, bodyLimit);
        if ((markersCount & 0xFFFFFFFFL) > 2097151L)
            throw new DecoderException("Invalid collection count");
        long markersAddress = storageAddress + 17L + PacketValues.varIntSize(markersCount);
        long markersRows = markersAddress;
        if (bodyLimit - markersAddress < (long) markersCount * 8)
            throw new DecoderException("Truncated packet collection");
        long markersEnd = markersAddress + (long) markersCount * 8L;
        long expireAt = ClientPacketEvents.markerExpiry(ttlMillis);
        decodedBuffer.readerIndex(bufferStart + (int) (markersEnd - storageAddress));
        for (int rowIndex = 0; rowIndex < markersCount; rowIndex++) {
            long positionWord = U.getLong(storageBase, markersRows);
            BlockPos position = of(positionWord);
            markersRows += 8L;
            Component label =
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(codecBuffer);
            ClientPacketEvents.marker(position, label, expireAt, maxDistance, color);
        }
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
    }
}
