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

import com.hbm.handler.radiation.RadVisSnapshot;
import com.hbm.handler.radiation.RadVisSyncHandlers;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.BitSet;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class RadVisSnapshotLayout {
    public static final int VARIANT = 2;
    public static final int VARIANT_BITS = 2;
    public static final int MIN_BODY_BYTES = 1;
    public static final int MAX_BODY_BYTES = 1048576;

    private RadVisSnapshotLayout() {}

    public static EncodedPayload encode(RadVisSnapshot snapshot, BitSet layout) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilySyncBodies.TYPE, 1, MAX_BODY_BYTES, true, false, false);
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
            {
                U.putByte(storageBase, storageAddress, (byte) (2L));
            }
            encodedBuffer.writerIndex(bufferStart + 1);
            snapshot.write(encodedBuffer, layout);
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
        if (((U.getByte(storageBase, storageAddress) & 255) >>> 2) != 0)
            throw new DecoderException("Nonzero packet padding");
        decodedBuffer.readerIndex(bufferStart + 1);
        ByteBuf snapshot = decodedBuffer;
        RadVisSyncHandlers.accept(snapshot);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
    }
}
