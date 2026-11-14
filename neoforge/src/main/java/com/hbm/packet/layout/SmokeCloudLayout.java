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

import com.hbm.client.effect.BurstEffects;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

public final class SmokeCloudLayout {
    public static final int VARIANT = 3;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 29;
    public static final int MAX_BODY_BYTES = 29;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private SmokeCloudLayout() {}

    public static EncodedPayload encode(double x, double y, double z, int count) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyEffects.TYPE, BODY_BYTES, MAX_BODY_BYTES, true, false, false);
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
            long xBits = doubleToRawLongBits(x);
            long yBits = doubleToRawLongBits(y);
            long zBits = doubleToRawLongBits(z);
            long countBits = count;
            U.putLong(storageBase, storageAddress, 3L | (xBits << 5));
            U.putLong(storageBase, storageAddress + 8L, (xBits >>> 59) | (yBits << 5));
            U.putLong(storageBase, storageAddress + 16L, (yBits >>> 59) | (zBits << 5));
            U.putLong(
                    storageBase,
                    storageAddress + 21L,
                    (zBits >>> 35) | ((countBits & 0xFFFFFFFFL) << 29));
            encodedBuffer.writerIndex(bufferStart + BODY_BYTES);
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
        if (bodyBytes != BODY_BYTES) throw new DecoderException("Invalid packet length");
        long xWord = U.getLong(storageBase, storageAddress);
        long xToYWord = U.getLong(storageBase, storageAddress + 8L);
        long yToZWord = U.getLong(storageBase, storageAddress + 16L);
        long zToCountWord = U.getLong(storageBase, storageAddress + 21L);
        double x = longBitsToDouble(((xWord >>> 5) | (xToYWord << 59)));
        double y = longBitsToDouble(((xToYWord >>> 5) | (yToZWord << 59)));
        double z = longBitsToDouble(((yToZWord >>> 5) | ((zToCountWord >>> 24) << 59)));
        int count = (int) (zToCountWord >>> 29);
        if ((zToCountWord >>> 61) != 0) throw new DecoderException("Nonzero packet padding");
        BurstEffects.smokeCloud(x, y, z, count);
    }
}
