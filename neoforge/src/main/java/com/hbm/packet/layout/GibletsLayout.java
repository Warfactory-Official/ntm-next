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
import com.hbm.particle.EffectKinds.GibletType;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

public final class GibletsLayout {
    public static final int VARIANT = 23;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 33;
    public static final int MAX_BODY_BYTES = 33;
    public static final int BODY_BYTES = MIN_BODY_BYTES;
    private static final GibletType[] gibTypeValues = GibletType.values();

    static {
        if (gibTypeValues.length != 3)
            throw new ExceptionInInitializerError("Enum table size changed: gibType");
    }

    private GibletsLayout() {}

    public static EncodedPayload encode(
            double x, double y, double z, int entityId, GibletType gibType, int countDivisor) {
        if (countDivisor < 0) throw new IllegalArgumentException("Invalid countDivisor");
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
            long entityIdBits = entityId;
            long gibTypeBits = gibType.ordinal();
            long countDivisorBits = (long) countDivisor;
            U.putLong(storageBase, storageAddress, 23L | (xBits << 5));
            U.putLong(storageBase, storageAddress + 8L, (xBits >>> 59) | (yBits << 5));
            U.putLong(storageBase, storageAddress + 16L, (yBits >>> 59) | (zBits << 5));
            U.putLong(
                    storageBase,
                    storageAddress + 24L,
                    (zBits >>> 59)
                            | ((entityIdBits & 0xFFFFFFFFL) << 5)
                            | (gibTypeBits << 37)
                            | (countDivisorBits << 39));
            U.putByte(storageBase, storageAddress + 32L, (byte) ((countDivisorBits >>> 25)));
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
        long zToCountDivisorWord = U.getLong(storageBase, storageAddress + 24L);
        long countDivisorWord = (U.getByte(storageBase, storageAddress + 32L) & 255L);
        double x = longBitsToDouble(((xWord >>> 5) | (xToYWord << 59)));
        double y = longBitsToDouble(((xToYWord >>> 5) | (yToZWord << 59)));
        double z = longBitsToDouble(((yToZWord >>> 5) | (zToCountDivisorWord << 59)));
        int entityId = (int) (zToCountDivisorWord >>> 5);
        long gibTypeBits = ((zToCountDivisorWord >>> 37) & 3L);
        if (gibTypeBits >= 3L) throw new DecoderException("Invalid gibType ordinal");
        GibletType gibType = gibTypeValues[(int) gibTypeBits];
        int countDivisor = (int) ((zToCountDivisorWord >>> 39) | ((countDivisorWord & 63L) << 25));
        if ((countDivisorWord >>> 6) != 0) throw new DecoderException("Nonzero packet padding");
        DetailEffects.giblets(x, y, z, entityId, gibType, countDivisor);
    }
}
