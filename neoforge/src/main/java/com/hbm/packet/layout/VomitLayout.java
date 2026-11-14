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
import com.hbm.particle.EffectKinds.VomitMode;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class VomitLayout {
    public static final int VARIANT = 20;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 9;
    public static final int MAX_BODY_BYTES = 9;
    public static final int BODY_BYTES = MIN_BODY_BYTES;
    private static final VomitMode[] modeValues = VomitMode.values();

    static {
        if (modeValues.length != 3)
            throw new ExceptionInInitializerError("Enum table size changed: mode");
    }

    private VomitLayout() {}

    public static EncodedPayload encode(int entityId, VomitMode mode, int count) {
        if (count < 0) throw new IllegalArgumentException("Invalid count");
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
            long entityIdBits = entityId;
            long modeBits = mode.ordinal();
            long countBits = (long) count;
            U.putLong(
                    storageBase,
                    storageAddress,
                    20L
                            | ((entityIdBits & 0xFFFFFFFFL) << 5)
                            | (modeBits << 37)
                            | (countBits << 39));
            U.putByte(storageBase, storageAddress + 8L, (byte) ((countBits >>> 25)));
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
        long entityIdToCountWord = U.getLong(storageBase, storageAddress);
        long countWord = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        int entityId = (int) (entityIdToCountWord >>> 5);
        long modeBits = ((entityIdToCountWord >>> 37) & 3L);
        if (modeBits >= 3L) throw new DecoderException("Invalid mode ordinal");
        VomitMode mode = modeValues[(int) modeBits];
        int count = (int) ((entityIdToCountWord >>> 39) | ((countWord & 63L) << 25));
        if ((countWord >>> 6) != 0) throw new DecoderException("Nonzero packet padding");
        DetailEffects.vomit(entityId, mode, count);
    }
}
