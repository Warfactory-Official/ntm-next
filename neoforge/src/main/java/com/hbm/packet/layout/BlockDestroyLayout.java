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

public final class BlockDestroyLayout {
    public static final int VARIANT = 16;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 17;
    public static final int MAX_BODY_BYTES = 17;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private BlockDestroyLayout() {}

    public static EncodedPayload encode(int x, int y, int z, int stateId) {
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
            long xBits = x;
            long yBits = y;
            long zBits = z;
            long stateIdBits = stateId;
            U.putLong(
                    storageBase,
                    storageAddress,
                    16L | ((xBits & 0xFFFFFFFFL) << 5) | (yBits << 37));
            U.putLong(
                    storageBase,
                    storageAddress + 8L,
                    ((yBits >>> 27) & 31L) | ((zBits & 0xFFFFFFFFL) << 5) | (stateIdBits << 37));
            U.putByte(storageBase, storageAddress + 16L, (byte) (((stateIdBits >>> 27) & 31L)));
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
        long xToYWord = U.getLong(storageBase, storageAddress);
        long yToStateIdWord = U.getLong(storageBase, storageAddress + 8L);
        long stateIdWord = (U.getByte(storageBase, storageAddress + 16L) & 255L);
        int x = (int) (xToYWord >>> 5);
        int y = (int) ((xToYWord >>> 37) | (yToStateIdWord << 27));
        int z = (int) (yToStateIdWord >>> 5);
        int stateId = (int) ((yToStateIdWord >>> 37) | (stateIdWord << 27));
        if ((stateIdWord >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        BurstEffects.blockDestroy(x, y, z, stateId);
    }
}
