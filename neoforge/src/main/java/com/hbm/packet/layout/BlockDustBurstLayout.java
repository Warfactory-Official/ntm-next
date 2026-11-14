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

public final class BlockDustBurstLayout {
    public static final int VARIANT = 15;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 41;
    public static final int MAX_BODY_BYTES = 41;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private BlockDustBurstLayout() {}

    public static EncodedPayload encode(
            int stateId, double x, double y, double z, int count, double motion) {
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
            long stateIdBits = stateId;
            long xBits = doubleToRawLongBits(x);
            long yBits = doubleToRawLongBits(y);
            long zBits = doubleToRawLongBits(z);
            long countBits = count;
            long motionBits = doubleToRawLongBits(motion);
            U.putLong(
                    storageBase,
                    storageAddress,
                    15L | ((stateIdBits & 0xFFFFFFFFL) << 5) | (xBits << 37));
            U.putLong(storageBase, storageAddress + 8L, (xBits >>> 27) | (yBits << 37));
            U.putLong(storageBase, storageAddress + 16L, (yBits >>> 27) | (zBits << 37));
            U.putLong(storageBase, storageAddress + 24L, (zBits >>> 27) | (countBits << 37));
            U.putLong(
                    storageBase,
                    storageAddress + 32L,
                    ((countBits >>> 27) & 31L) | (motionBits << 5));
            U.putByte(storageBase, storageAddress + 40L, (byte) ((motionBits >>> 59)));
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
        long stateIdToXWord = U.getLong(storageBase, storageAddress);
        long xToYWord = U.getLong(storageBase, storageAddress + 8L);
        long yToZWord = U.getLong(storageBase, storageAddress + 16L);
        long zToCountWord = U.getLong(storageBase, storageAddress + 24L);
        long countToMotionWord = U.getLong(storageBase, storageAddress + 32L);
        long motionWord = (U.getByte(storageBase, storageAddress + 40L) & 255L);
        int stateId = (int) (stateIdToXWord >>> 5);
        double x = longBitsToDouble(((stateIdToXWord >>> 37) | (xToYWord << 27)));
        double y = longBitsToDouble(((xToYWord >>> 37) | (yToZWord << 27)));
        double z = longBitsToDouble(((yToZWord >>> 37) | (zToCountWord << 27)));
        int count = (int) ((zToCountWord >>> 37) | (countToMotionWord << 27));
        double motion = longBitsToDouble(((countToMotionWord >>> 5) | (motionWord << 59)));
        if ((motionWord >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        BurstEffects.blockDustBurst(stateId, x, y, z, count, motion);
    }
}
