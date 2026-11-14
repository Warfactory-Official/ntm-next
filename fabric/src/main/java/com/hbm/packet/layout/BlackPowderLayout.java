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
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

public final class BlackPowderLayout {
    public static final int VARIANT = 12;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 69;
    public static final int MAX_BODY_BYTES = 69;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private BlackPowderLayout() {}

    public static EncodedPayload encode(
            double x,
            double y,
            double z,
            double hx,
            double hy,
            double hz,
            int cloudCount,
            float cloudScale,
            float cloudSpeedMult,
            int sparkCount,
            float sparkSpeedMult) {
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
            long hxBits = doubleToRawLongBits(hx);
            long hyBits = doubleToRawLongBits(hy);
            long hzBits = doubleToRawLongBits(hz);
            long cloudCountBits = cloudCount;
            long cloudScaleBits = floatToRawIntBits(cloudScale);
            long cloudSpeedMultBits = floatToRawIntBits(cloudSpeedMult);
            long sparkCountBits = sparkCount;
            long sparkSpeedMultBits = floatToRawIntBits(sparkSpeedMult);
            U.putLong(storageBase, storageAddress, 12L | (xBits << 5));
            U.putLong(storageBase, storageAddress + 8L, (xBits >>> 59) | (yBits << 5));
            U.putLong(storageBase, storageAddress + 16L, (yBits >>> 59) | (zBits << 5));
            U.putLong(storageBase, storageAddress + 24L, (zBits >>> 59) | (hxBits << 5));
            U.putLong(storageBase, storageAddress + 32L, (hxBits >>> 59) | (hyBits << 5));
            U.putLong(storageBase, storageAddress + 40L, (hyBits >>> 59) | (hzBits << 5));
            U.putLong(
                    storageBase,
                    storageAddress + 48L,
                    (hzBits >>> 59)
                            | ((cloudCountBits & 0xFFFFFFFFL) << 5)
                            | (cloudScaleBits << 37));
            U.putLong(
                    storageBase,
                    storageAddress + 56L,
                    ((cloudScaleBits >>> 27) & 31L)
                            | ((cloudSpeedMultBits & 0xFFFFFFFFL) << 5)
                            | (sparkCountBits << 37));
            U.putLong(
                    storageBase,
                    storageAddress + 61L,
                    ((sparkCountBits >>> 3) & 0x1FFFFFFFL)
                            | ((sparkSpeedMultBits & 0xFFFFFFFFL) << 29));
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
        long zToHxWord = U.getLong(storageBase, storageAddress + 24L);
        long hxToHyWord = U.getLong(storageBase, storageAddress + 32L);
        long hyToHzWord = U.getLong(storageBase, storageAddress + 40L);
        long hzToCloudScaleWord = U.getLong(storageBase, storageAddress + 48L);
        long cloudScaleToSparkCountWord = U.getLong(storageBase, storageAddress + 56L);
        long sparkCountToSparkSpeedMultWord = U.getLong(storageBase, storageAddress + 61L);
        double x = longBitsToDouble(((xWord >>> 5) | (xToYWord << 59)));
        double y = longBitsToDouble(((xToYWord >>> 5) | (yToZWord << 59)));
        double z = longBitsToDouble(((yToZWord >>> 5) | (zToHxWord << 59)));
        double hx = longBitsToDouble(((zToHxWord >>> 5) | (hxToHyWord << 59)));
        double hy = longBitsToDouble(((hxToHyWord >>> 5) | (hyToHzWord << 59)));
        double hz = longBitsToDouble(((hyToHzWord >>> 5) | (hzToCloudScaleWord << 59)));
        int cloudCount = (int) (hzToCloudScaleWord >>> 5);
        float cloudScale =
                intBitsToFloat(
                        (int) ((hzToCloudScaleWord >>> 37) | (cloudScaleToSparkCountWord << 27)));
        float cloudSpeedMult = intBitsToFloat((int) (cloudScaleToSparkCountWord >>> 5));
        int sparkCount =
                (int)
                        ((cloudScaleToSparkCountWord >>> 37)
                                | ((sparkCountToSparkSpeedMultWord >>> 24) << 27));
        float sparkSpeedMult = intBitsToFloat((int) (sparkCountToSparkSpeedMultWord >>> 29));
        if ((sparkCountToSparkSpeedMultWord >>> 61) != 0)
            throw new DecoderException("Nonzero packet padding");
        BurstEffects.blackPowder(
                x,
                y,
                z,
                hx,
                hy,
                hz,
                cloudCount,
                cloudScale,
                cloudSpeedMult,
                sparkCount,
                sparkSpeedMult);
    }
}
