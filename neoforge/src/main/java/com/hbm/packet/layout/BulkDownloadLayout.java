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

import com.hbm.packet.BulkTransfers;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class BulkDownloadLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 0;
    public static final int MIN_BODY_BYTES = 23;
    public static final int MAX_BODY_BYTES = 16408;

    private BulkDownloadLayout() {}

    public static EncodedPayload encode(
            long message,
            int total,
            int rawLength,
            int index,
            boolean compressed,
            byte[] data,
            int offset) {
        int dataCount = Math.min(16 * 1024, total - offset);
        if (dataCount < 1 || dataCount > 16384)
            throw new IllegalArgumentException("Invalid data count");
        int dataCountBytes = dataCount < 128 ? 1 : dataCount < 16384 ? 2 : 3;
        int bodyBytes = 21 + dataCountBytes + dataCount;
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyBulkDownload.TYPE,
                        bodyBytes,
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
            long messageBits = message;
            long totalBits = total;
            long rawLengthBits = rawLength;
            long indexBits = index;
            long compressedBits = compressed ? 1L : 0L;
            U.putLong(storageBase, storageAddress, messageBits);
            U.putLong(
                    storageBase,
                    storageAddress + 8L,
                    (totalBits & 0xFFFFFFFFL) | (rawLengthBits << 32));
            U.putLong(
                    storageBase,
                    storageAddress + 13L,
                    ((rawLengthBits >>> 8) & 0xFFFFFFL)
                            | ((indexBits & 0xFFFFFFFFL) << 24)
                            | (compressedBits << 56));
            if (dataCount < 128) {
                U.putByte(storageBase, storageAddress + 21L, (byte) (dataCount));
            } else {
                U.putByte(storageBase, storageAddress + 21L, (byte) (dataCount | 0x80));
                if (dataCount < 16384) {
                    U.putByte(storageBase, storageAddress + 22L, (byte) ((dataCount >>> 7)));
                } else {
                    U.putByte(storageBase, storageAddress + 22L, (byte) ((dataCount >>> 7) | 0x80));
                    U.putByte(storageBase, storageAddress + 23L, (byte) ((dataCount >>> 14)));
                }
            }
            long dataAddress = storageAddress + 21L + dataCountBytes;
            for (int elementIndex = 0; elementIndex < dataCount; elementIndex++) {
                U.putByte(storageBase, dataAddress, (byte) (data[offset + elementIndex]));
                dataAddress += 1L;
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
        long messageWord = U.getLong(storageBase, storageAddress);
        long totalToRawLengthWord = U.getLong(storageBase, storageAddress + 8L);
        long indexToCompressedWord = U.getLong(storageBase, storageAddress + 13L);
        long message = messageWord;
        int total = (int) totalToRawLengthWord;
        int rawLength = (int) (totalToRawLengthWord >>> 32);
        int index = (int) (indexToCompressedWord >>> 24);
        boolean compressed = ((indexToCompressedWord >>> 56) & 1L) != 0;
        if ((indexToCompressedWord >>> 57) != 0)
            throw new DecoderException("Nonzero packet padding");
        int dataCount = U.getByte(storageBase, storageAddress + 21L) & 255;
        int dataOffset = 22;
        if ((dataCount & 0x80) != 0) {
            int dataCountByte2 = U.getByte(storageBase, storageAddress + 22L) & 255;
            dataCount = (dataCount & 127) | (dataCountByte2 << 7);
            dataOffset = 23;
            if ((dataCountByte2 & 0x80) != 0) {
                if (bodyBytes < 24) throw new DecoderException("Truncated collection count");
                int dataCountByte3 = U.getByte(storageBase, storageAddress + 23L) & 255;
                dataCount = (dataCount & 16383) | (dataCountByte3 << 14);
                dataOffset = 24;
                if ((dataCountByte3 & 0x80) != 0) {
                    throw new DecoderException("Invalid collection count");
                }
                if (dataCountByte3 == 0)
                    throw new DecoderException("Noncanonical collection count");
            }
            if (dataCountByte2 == 0) throw new DecoderException("Noncanonical collection count");
        }
        if (dataOffset + dataCount != bodyBytes) throw new DecoderException("Invalid data length");
        long dataAddress = storageAddress + dataOffset;
        byte[] data = new byte[dataCount];
        U.copyMemory(storageBase, dataAddress, data, BA_BASE, dataCount);
        BulkTransfers.download(context, message, total, rawLength, index, compressed, data);
    }
}
