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

import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static net.minecraft.core.BlockPos.of;

public final class FluidPipeTintLayout {
    public static final int VARIANT = 1;
    public static final int VARIANT_BITS = 3;
    public static final int MIN_BODY_BYTES = 13;
    public static final int MAX_BODY_BYTES = 13;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private FluidPipeTintLayout() {}

    public static EncodedPayload encode(BlockPos pos, int fluidId) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilySyncValues.TYPE,
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
            long posBits = pos.asLong();
            long fluidIdBits = fluidId;
            U.putLong(storageBase, storageAddress, 1L | (posBits << 3));
            U.putLong(
                    storageBase,
                    storageAddress + 5L,
                    (posBits >>> 37) | ((fluidIdBits & 0xFFFFFFFFL) << 27));
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
        long posWord = U.getLong(storageBase, storageAddress);
        long posToFluidIdWord = U.getLong(storageBase, storageAddress + 5L);
        BlockPos pos = of(((posWord >>> 3) | ((posToFluidIdWord >>> 24) << 61)));
        int fluidId = (int) (posToFluidIdWord >>> 27);
        if ((posToFluidIdWord >>> 59) != 0) throw new DecoderException("Nonzero packet padding");
        var player = context.playerOrNull();
        if (player == null) return;
        FluidPipeTintData.acceptClient(player, pos, fluidId);
    }
}
