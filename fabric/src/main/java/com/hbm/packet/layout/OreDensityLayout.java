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
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class OreDensityLayout {
    public static final int VARIANT = 30;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 11;
    public static final int MAX_BODY_BYTES = 1048576;

    private OreDensityLayout() {}

    public static EncodedPayload encode(
            int firstId, int millis, List<Component> rows, Component summary) {
        int rowsCount = rows.size();
        if (rowsCount > 2097151) throw new IllegalArgumentException("Invalid rows count");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyClientEvents.TYPE, 14, MAX_BODY_BYTES, true, false, false);
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
                long firstIdBits = firstId;
                long millisBits = millis;
                U.putLong(
                        storageBase,
                        storageAddress,
                        30L | ((firstIdBits & 0xFFFFFFFFL) << 5) | (millisBits << 37));
                U.putByte(storageBase, storageAddress + 8L, (byte) (((millisBits >>> 27) & 31L)));
            }
            long rowsAddress = PacketValues.putVarInt(storageBase, storageAddress + 9L, rowsCount);
            encodedBuffer.writerIndex(bufferStart + (int) (rowsAddress - storageAddress));
            for (int elementIndex = 0; elementIndex < rowsCount; elementIndex++) {
                ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(
                        codecBuffer, rows.get(elementIndex));
            }
            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(codecBuffer, summary);
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
        long firstIdToMillisWord = U.getLong(storageBase, storageAddress);
        long millisWord = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        int firstId = (int) (firstIdToMillisWord >>> 5);
        int millis = (int) ((firstIdToMillisWord >>> 37) | (millisWord << 27));
        if ((millisWord >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        int rowsCount = PacketValues.getVarInt(storageBase, storageAddress + 9L, bodyLimit);
        if ((rowsCount & 0xFFFFFFFFL) > 2097151L)
            throw new DecoderException("Invalid collection count");
        long rowsAddress = storageAddress + 9L + PacketValues.varIntSize(rowsCount);
        decodedBuffer.readerIndex(bufferStart + (int) (rowsAddress - storageAddress));
        for (int elementIndex = 0; elementIndex < rowsCount; elementIndex++) {
            Component row =
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(codecBuffer);
            ClientPacketEvents.oreDensityRow(firstId + elementIndex, millis, row);
        }
        Component summary =
                ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(codecBuffer);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        ClientPacketEvents.oreDensity(firstId, millis, rowsCount, summary);
    }
}
