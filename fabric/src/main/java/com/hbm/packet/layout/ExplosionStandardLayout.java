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

import com.hbm.client.effect.DetailEffects;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.List;
import net.minecraft.core.BlockPos;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

public final class ExplosionStandardLayout {
    public static final int VARIANT = 29;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 18;
    public static final int MAX_BODY_BYTES = 1048572;

    private ExplosionStandardLayout() {}

    public static EncodedPayload encode(
            float x, float y, float z, float size, List<BlockPos> blocks) {
        int blocksCount = blocks.size();
        if (blocksCount > 131069) throw new EncoderException("Invalid blocks count");
        int blocksCountBytes = blocksCount < 128 ? 1 : blocksCount < 16384 ? 2 : 3;
        int bodyBytes = 17 + blocksCountBytes + (blocksCount << 3);
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyEffects.TYPE, bodyBytes, MAX_BODY_BYTES, true, false, false);
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
            long xBits = floatToRawIntBits(x);
            long yBits = floatToRawIntBits(y);
            long zBits = floatToRawIntBits(z);
            long sizeBits = floatToRawIntBits(size);
            U.putLong(
                    storageBase,
                    storageAddress,
                    29L | ((xBits & 0xFFFFFFFFL) << 5) | (yBits << 37));
            U.putLong(
                    storageBase,
                    storageAddress + 8L,
                    ((yBits >>> 27) & 31L) | ((zBits & 0xFFFFFFFFL) << 5) | (sizeBits << 37));
            U.putByte(storageBase, storageAddress + 16L, (byte) (((sizeBits >>> 27) & 31L)));
            if (blocksCount < 128) {
                U.putByte(storageBase, storageAddress + 17L, (byte) (blocksCount));
            } else {
                U.putByte(storageBase, storageAddress + 17L, (byte) (blocksCount | 0x80));
                if (blocksCount < 16384) {
                    U.putByte(storageBase, storageAddress + 18L, (byte) ((blocksCount >>> 7)));
                } else {
                    U.putByte(
                            storageBase, storageAddress + 18L, (byte) ((blocksCount >>> 7) | 0x80));
                    U.putByte(storageBase, storageAddress + 19L, (byte) ((blocksCount >>> 14)));
                }
            }
            long blocksAddress = storageAddress + 17L + blocksCountBytes;
            for (int elementIndex = 0; elementIndex < blocksCount; elementIndex++) {
                U.putLong(storageBase, blocksAddress, blocks.get(elementIndex).asLong());
                blocksAddress += 8L;
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
        long xToYWord = U.getLong(storageBase, storageAddress);
        long yToSizeWord = U.getLong(storageBase, storageAddress + 8L);
        long sizeWord = (U.getByte(storageBase, storageAddress + 16L) & 255L);
        float x = intBitsToFloat((int) (xToYWord >>> 5));
        float y = intBitsToFloat((int) ((xToYWord >>> 37) | (yToSizeWord << 27)));
        float z = intBitsToFloat((int) (yToSizeWord >>> 5));
        float size = intBitsToFloat((int) ((yToSizeWord >>> 37) | (sizeWord << 27)));
        if ((sizeWord >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        int blocksCount = U.getByte(storageBase, storageAddress + 17L) & 255;
        int blocksOffset = 18;
        if ((blocksCount & 0x80) != 0) {
            if (bodyBytes < 19) throw new DecoderException("Truncated collection count");
            int blocksCountByte2 = U.getByte(storageBase, storageAddress + 18L) & 255;
            blocksCount = (blocksCount & 127) | (blocksCountByte2 << 7);
            blocksOffset = 19;
            if ((blocksCountByte2 & 0x80) != 0) {
                if (bodyBytes < 20) throw new DecoderException("Truncated collection count");
                int blocksCountByte3 = U.getByte(storageBase, storageAddress + 19L) & 255;
                blocksCount = (blocksCount & 16383) | (blocksCountByte3 << 14);
                blocksOffset = 20;
                if ((blocksCountByte3 & 0x80) != 0) {
                    throw new DecoderException("Invalid collection count");
                }
                if (blocksCountByte3 == 0)
                    throw new DecoderException("Noncanonical collection count");
            }
            if (blocksCountByte2 == 0) throw new DecoderException("Noncanonical collection count");
        }
        if (blocksOffset + (blocksCount << 3) != bodyBytes)
            throw new DecoderException("Invalid blocks length");
        long blocksAddress = storageAddress + blocksOffset;
        DetailEffects.explosionStandard(x, y, z, size, storageBase, blocksAddress, blocksCount);
    }
}
