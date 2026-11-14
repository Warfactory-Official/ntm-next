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

import com.hbm.client.ClientPacketEvents;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

public final class ImpactSyncLayout {
    public static final int VARIANT = 22;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 9;
    public static final int MAX_BODY_BYTES = 9;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private ImpactSyncLayout() {}

    public static EncodedPayload encode(float fire, float dust) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyClientEvents.TYPE,
                        BODY_BYTES,
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
            long fireBits = floatToRawIntBits(fire);
            long dustBits = floatToRawIntBits(dust);
            U.putLong(
                    storageBase,
                    storageAddress,
                    22L | ((fireBits & 0xFFFFFFFFL) << 5) | (dustBits << 37));
            U.putByte(storageBase, storageAddress + 8L, (byte) (((dustBits >>> 27) & 31L)));
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
        long fireToDustWord = U.getLong(storageBase, storageAddress);
        long dustWord = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        float fire = intBitsToFloat((int) (fireToDustWord >>> 5));
        float dust = intBitsToFloat((int) ((fireToDustWord >>> 37) | (dustWord << 27)));
        if ((dustWord >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        var player = context.playerOrNull();
        if (player == null) return;
        ClientPacketEvents.impactSync(player, fire, dust);
    }
}
