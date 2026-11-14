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

import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.action.ServerActions;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class DesignatorCoordinatesLayout {
    public static final int VARIANT = 6;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 6;
    public static final int MAX_BODY_BYTES = 6;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private DesignatorCoordinatesLayout() {}

    public static EncodedPayload encode(int containerId, int operator, int value, int reference) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        if (((long) operator & 0xFFFFFFFFL) > 2L)
            throw new IllegalArgumentException("Invalid operator");
        if (((long) reference & 0xFFFFFFFFL) > 1L)
            throw new IllegalArgumentException("Invalid reference");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyServerActions.TYPE,
                        BODY_BYTES,
                        MAX_BODY_BYTES,
                        false,
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
            long containerIdBits = ((long) containerId - 1L);
            long operatorBits = (long) operator;
            long valueBits = value;
            long referenceBits = (long) reference;
            U.putInt(
                    storageBase,
                    storageAddress,
                    (int) (6L | (containerIdBits << 5) | (operatorBits << 12) | (valueBits << 14)));
            U.putShort(
                    storageBase,
                    storageAddress + 4L,
                    (short) (((valueBits >>> 18) & 0x3FFFL) | (referenceBits << 14)));
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
        long containerIdToValueWord = (U.getInt(storageBase, storageAddress) & 0xFFFFFFFFL);
        long valueToReferenceWord = (U.getShort(storageBase, storageAddress + 4L) & 65535L);
        long containerIdBits = ((containerIdToValueWord >>> 5) & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        long operatorBits = ((containerIdToValueWord >>> 12) & 3L);
        if (operatorBits > 2L) throw new DecoderException("Invalid operator");
        int operator = (int) operatorBits;
        int value = (int) ((containerIdToValueWord >>> 14) | (valueToReferenceWord << 18));
        int reference = (int) ((valueToReferenceWord >>> 14) & 1L);
        if ((valueToReferenceWord >>> 15) != 0)
            throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)) return;
        ServerActions.designatorCoordinates(player, containerId, operator, value, reference);
    }
}
