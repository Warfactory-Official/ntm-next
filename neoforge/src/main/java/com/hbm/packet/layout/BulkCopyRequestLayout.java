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
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.world.bulk.BulkCopyRequests;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class BulkCopyRequestLayout {
    public static final int VARIANT = 2;
    public static final int VARIANT_BITS = 2;
    public static final int MIN_BODY_BYTES = 17;
    public static final int MAX_BODY_BYTES = 17;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private BulkCopyRequestLayout() {}

    public static EncodedPayload encode(long request, long chunk) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyBulkSession.TYPE,
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
            long requestBits = request;
            long chunkBits = chunk;
            U.putLong(storageBase, storageAddress, 2L | (requestBits << 2));
            U.putLong(storageBase, storageAddress + 8L, (requestBits >>> 62) | (chunkBits << 2));
            U.putByte(storageBase, storageAddress + 16L, (byte) ((chunkBits >>> 62)));
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
        long requestWord = U.getLong(storageBase, storageAddress);
        long requestToChunkWord = U.getLong(storageBase, storageAddress + 8L);
        long chunkWord = (U.getByte(storageBase, storageAddress + 16L) & 255L);
        long request = ((requestWord >>> 2) | (requestToChunkWord << 62));
        long chunk = ((requestToChunkWord >>> 2) | (chunkWord << 62));
        if ((chunkWord >>> 2) != 0) throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)) return;
        BulkCopyRequests.serve(player, request, chunk);
    }
}
