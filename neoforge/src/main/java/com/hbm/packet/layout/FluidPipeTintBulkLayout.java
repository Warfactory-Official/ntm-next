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

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.longs.LongList;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class FluidPipeTintBulkLayout {
    public static final int VARIANT = 2;
    public static final int VARIANT_BITS = 3;
    public static final int MIN_BODY_BYTES = 6;
    public static final int MAX_BODY_BYTES = 1048576;

    private FluidPipeTintBulkLayout() {}

    public static EncodedPayload encode(int fluidId, LongList positions) {
        int positionsCount = positions.size();
        if (positionsCount > 131071) throw new EncoderException("Invalid positions count");
        int positionsCountBytes = positionsCount < 128 ? 1 : positionsCount < 16384 ? 2 : 3;
        int bodyBytes = 5 + positionsCountBytes + (positionsCount << 3);
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilySyncValues.TYPE, bodyBytes, MAX_BODY_BYTES, true, false, false);
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
            long fluidIdBits = fluidId;
            U.putInt(storageBase, storageAddress, (int) (2L | (fluidIdBits << 3)));
            U.putByte(storageBase, storageAddress + 4L, (byte) (((fluidIdBits >>> 29) & 7L)));
            if (positionsCount < 128) {
                U.putByte(storageBase, storageAddress + 5L, (byte) (positionsCount));
            } else {
                U.putByte(storageBase, storageAddress + 5L, (byte) (positionsCount | 0x80));
                if (positionsCount < 16384) {
                    U.putByte(storageBase, storageAddress + 6L, (byte) ((positionsCount >>> 7)));
                } else {
                    U.putByte(
                            storageBase,
                            storageAddress + 6L,
                            (byte) ((positionsCount >>> 7) | 0x80));
                    U.putByte(storageBase, storageAddress + 7L, (byte) ((positionsCount >>> 14)));
                }
            }
            long positionsAddress = storageAddress + 5L + positionsCountBytes;
            for (int elementIndex = 0; elementIndex < positionsCount; elementIndex++) {
                U.putLong(storageBase, positionsAddress, positions.getLong(elementIndex));
                positionsAddress += 8L;
            }
            encodedBuffer.writerIndex(bufferStart + bodyBytes);
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
        if (bodyBytes < MIN_BODY_BYTES || bodyBytes > MAX_BODY_BYTES)
            throw new DecoderException("Invalid packet length");
        long fluidIdWord = (U.getInt(storageBase, storageAddress) & 0xFFFFFFFFL);
        long fluidIdWordPart2 = (U.getByte(storageBase, storageAddress + 4L) & 255L);
        int fluidId = (int) ((fluidIdWord >>> 3) | (fluidIdWordPart2 << 29));
        if ((fluidIdWordPart2 >>> 3) != 0) throw new DecoderException("Nonzero packet padding");
        int positionsCount = U.getByte(storageBase, storageAddress + 5L) & 255;
        int positionsOffset = 6;
        if ((positionsCount & 0x80) != 0) {
            if (bodyBytes < 7) throw new DecoderException("Truncated collection count");
            int positionsCountByte2 = U.getByte(storageBase, storageAddress + 6L) & 255;
            positionsCount = (positionsCount & 127) | (positionsCountByte2 << 7);
            positionsOffset = 7;
            if ((positionsCountByte2 & 0x80) != 0) {
                if (bodyBytes < 8) throw new DecoderException("Truncated collection count");
                int positionsCountByte3 = U.getByte(storageBase, storageAddress + 7L) & 255;
                positionsCount = (positionsCount & 16383) | (positionsCountByte3 << 14);
                positionsOffset = 8;
                if ((positionsCountByte3 & 0x80) != 0) {
                    throw new DecoderException("Invalid collection count");
                }
                if (positionsCountByte3 == 0)
                    throw new DecoderException("Noncanonical collection count");
            }
            if (positionsCountByte2 == 0)
                throw new DecoderException("Noncanonical collection count");
        }
        if (positionsOffset + (positionsCount << 3) != bodyBytes)
            throw new DecoderException("Invalid positions length");
        long positionsAddress = storageAddress + positionsOffset;
        var player = context.playerOrNull();
        if (player == null) return;
        FluidPipeTintData.acceptClient(
                player, fluidId, storageBase, positionsAddress, positionsCount);
    }
}
