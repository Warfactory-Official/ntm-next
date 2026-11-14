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
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class FluidPipeTintRequestLayout {
    public static final int VARIANT = 9;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 2;
    public static final int MAX_BODY_BYTES = 32763;

    private FluidPipeTintRequestLayout() {}

    public static EncodedPayload encode(LongList positions) {
        int positionsCount = positions.size();
        if (positionsCount > 4095) throw new EncoderException("Invalid positions count");
        int positionsCountBytes = positionsCount < 128 ? 1 : 2;
        int bodyBytes = 1 + positionsCountBytes + (positionsCount << 3);
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyServerActions.TYPE,
                        bodyBytes,
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
            U.putByte(storageBase, storageAddress, (byte) (9L));
            if (positionsCount < 128) {
                U.putByte(storageBase, storageAddress + 1L, (byte) (positionsCount));
            } else {
                U.putByte(storageBase, storageAddress + 1L, (byte) (positionsCount | 0x80));
                U.putByte(storageBase, storageAddress + 2L, (byte) ((positionsCount >>> 7)));
            }
            long positionsAddress = storageAddress + 1L + positionsCountBytes;
            for (int elementIndex = 0; elementIndex < positionsCount; elementIndex++) {
                U.putLong(storageBase, positionsAddress, positions.getLong(elementIndex));
                positionsAddress += 8L;
            }
            encodedBuffer.writerIndex(bufferStart + bodyBytes);
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
        if (bodyBytes < MIN_BODY_BYTES || bodyBytes > MAX_BODY_BYTES)
            throw new DecoderException("Invalid packet length");
        if ((((U.getByte(storageBase, storageAddress) & 255L) >>> 5)) != 0)
            throw new DecoderException("Nonzero packet padding");
        int positionsCount = U.getByte(storageBase, storageAddress + 1L) & 255;
        int positionsOffset = 2;
        if ((positionsCount & 0x80) != 0) {
            if (bodyBytes < 3) throw new DecoderException("Truncated collection count");
            int positionsCountByte2 = U.getByte(storageBase, storageAddress + 2L) & 255;
            positionsCount = (positionsCount & 127) | (positionsCountByte2 << 7);
            positionsOffset = 3;
            if ((positionsCountByte2 & 0x80) != 0) {
                throw new DecoderException("Invalid collection count");
            }
            if (positionsCountByte2 == 0)
                throw new DecoderException("Noncanonical collection count");
        }
        if (positionsOffset + (positionsCount << 3) != bodyBytes)
            throw new DecoderException("Invalid positions length");
        long positionsAddress = storageAddress + positionsOffset;
        if (!(context.player() instanceof ServerPlayer player)) return;
        ServerActions.fluidPipeTintRequest(player, storageBase, positionsAddress, positionsCount);
    }
}
