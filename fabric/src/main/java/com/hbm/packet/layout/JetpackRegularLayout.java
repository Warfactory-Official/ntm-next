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

public final class JetpackRegularLayout {
    public static final int VARIANT = 9;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 5;
    public static final int MAX_BODY_BYTES = 5;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private JetpackRegularLayout() {}

    public static EncodedPayload encode(int playerId) {
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
            long playerIdBits = playerId;
            U.putInt(storageBase, storageAddress, (int) (9L | (playerIdBits << 5)));
            U.putByte(storageBase, storageAddress + 4L, (byte) (((playerIdBits >>> 27) & 31L)));
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
        long playerIdWord = (U.getInt(storageBase, storageAddress) & 0xFFFFFFFFL);
        long playerIdWordPart2 = (U.getByte(storageBase, storageAddress + 4L) & 255L);
        int playerId = (int) ((playerIdWord >>> 5) | (playerIdWordPart2 << 27));
        if ((playerIdWordPart2 >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        var player = context.playerOrNull();
        if (player == null) return;
        ClientPacketEvents.jetpackRegular(player, playerId);
    }
}
