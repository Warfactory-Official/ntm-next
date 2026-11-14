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

public final class GunAnimationLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 21;
    public static final int MAX_BODY_BYTES = 21;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private GunAnimationLayout() {}

    public static EncodedPayload encode(
            int type, int receiverIndex, int itemIndex, int sentSlot, int sentItem) {
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
            long typeBits = type;
            long receiverIndexBits = receiverIndex;
            long itemIndexBits = itemIndex;
            long sentSlotBits = sentSlot;
            long sentItemBits = sentItem;
            U.putLong(
                    storageBase,
                    storageAddress,
                    ((typeBits & 0xFFFFFFFFL) << 5) | (receiverIndexBits << 37));
            U.putLong(
                    storageBase,
                    storageAddress + 8L,
                    ((receiverIndexBits >>> 27) & 31L)
                            | ((itemIndexBits & 0xFFFFFFFFL) << 5)
                            | (sentSlotBits << 37));
            U.putLong(
                    storageBase,
                    storageAddress + 13L,
                    ((sentSlotBits >>> 3) & 0x1FFFFFFFL) | ((sentItemBits & 0xFFFFFFFFL) << 29));
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
        long typeToReceiverIndexWord = U.getLong(storageBase, storageAddress);
        long receiverIndexToSentSlotWord = U.getLong(storageBase, storageAddress + 8L);
        long sentSlotToSentItemWord = U.getLong(storageBase, storageAddress + 13L);
        int type = (int) (typeToReceiverIndexWord >>> 5);
        int receiverIndex =
                (int) ((typeToReceiverIndexWord >>> 37) | (receiverIndexToSentSlotWord << 27));
        int itemIndex = (int) (receiverIndexToSentSlotWord >>> 5);
        int sentSlot =
                (int)
                        ((receiverIndexToSentSlotWord >>> 37)
                                | ((sentSlotToSentItemWord >>> 24) << 27));
        int sentItem = (int) (sentSlotToSentItemWord >>> 29);
        if ((sentSlotToSentItemWord >>> 61) != 0)
            throw new DecoderException("Nonzero packet padding");
        ClientPacketEvents.gunAnimation(type, receiverIndex, itemIndex, sentSlot, sentItem);
    }
}
