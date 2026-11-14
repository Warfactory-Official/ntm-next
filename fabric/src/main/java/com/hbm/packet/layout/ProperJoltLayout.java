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

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class ProperJoltLayout {
    public static final int VARIANT = 22;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 9;
    public static final int MAX_BODY_BYTES = 9;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private ProperJoltLayout() {}

    public static EncodedPayload encode(int time, int maxTime) {
        if (time < 0) throw new IllegalArgumentException("Invalid time");
        if (maxTime < 0) throw new IllegalArgumentException("Invalid maxTime");
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
            long timeBits = (long) time;
            long maxTimeBits = (long) maxTime;
            U.putLong(storageBase, storageAddress, 22L | (timeBits << 5) | (maxTimeBits << 36));
            U.putByte(storageBase, storageAddress + 8L, (byte) ((maxTimeBits >>> 28)));
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
        long timeToMaxTimeWord = U.getLong(storageBase, storageAddress);
        long maxTimeWord = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        int time = (int) ((timeToMaxTimeWord >>> 5) & 0x7FFFFFFFL);
        int maxTime = (int) ((timeToMaxTimeWord >>> 36) | ((maxTimeWord & 7L) << 28));
        if ((maxTimeWord >>> 3) != 0) throw new DecoderException("Nonzero packet padding");
        DetailEffects.properJolt(time, maxTime);
    }
}
