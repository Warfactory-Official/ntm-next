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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class BulkCopyLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 0;
    public static final int MIN_BODY_BYTES = 10;
    public static final int MAX_BODY_BYTES = 2147483647;

    private BulkCopyLayout() {}

    public static EncodedPayload encode(
            long request, boolean success, CompoundTag data, boolean finished) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyBulkCopy.TYPE, 10, MAX_BODY_BYTES, true, false, false);
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
            ByteBuf codecBuffer = encodedBuffer;
            {
                long requestBits = request;
                long successBits = success ? 1L : 0L;
                long finishedBits = finished ? 1L : 0L;
                U.putLong(storageBase, storageAddress, requestBits);
                U.putByte(
                        storageBase,
                        storageAddress + 8L,
                        (byte) (successBits | (finishedBits << 1)));
            }
            encodedBuffer.writerIndex(bufferStart + 9);
            ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(codecBuffer, data);
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
        if (bodyBytes < MIN_BODY_BYTES) throw new DecoderException("Invalid packet length");
        ByteBuf codecBuffer = decodedBuffer;
        long requestWord = U.getLong(storageBase, storageAddress);
        long successToFinishedWord = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        long request = requestWord;
        boolean success = (successToFinishedWord & 1L) != 0;
        boolean finished = ((successToFinishedWord >>> 1) & 1L) != 0;
        if ((successToFinishedWord >>> 2) != 0)
            throw new DecoderException("Nonzero packet padding");
        decodedBuffer.readerIndex(bufferStart + 9);
        CompoundTag data = ByteBufCodecs.TRUSTED_COMPOUND_TAG.decode(codecBuffer);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        BulkCopyRequests.accept(request, success, data, finished);
    }
}
