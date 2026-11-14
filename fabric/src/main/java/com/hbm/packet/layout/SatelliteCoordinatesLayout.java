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

public final class SatelliteCoordinatesLayout {
    public static final int VARIANT = 10;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 10;
    public static final int MAX_BODY_BYTES = 14;

    private SatelliteCoordinatesLayout() {}

    public static EncodedPayload encode(int containerId, int x, int z, boolean yPresent, int y) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        int bodyBytes = yPresent ? 14 : 10;
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
            long containerIdBits = ((long) containerId - 1L);
            long xBits = x;
            long zBits = z;
            U.putLong(
                    storageBase,
                    storageAddress,
                    10L | (containerIdBits << 5) | ((xBits & 0xFFFFFFFFL) << 12) | (zBits << 44));
            U.putByte(storageBase, storageAddress + 8L, (byte) ((zBits >>> 20)));
            long settingsWord = ((zBits >>> 28) & 15L) | ((yPresent ? 1L : 0L) << 4);
            if (yPresent) {
                long yBits = y & 0xFFFFFFFFL;
                settingsWord |= (yBits << (5));
            }
            if (bodyBytes == 10) {
                U.putByte(storageBase, storageAddress + 9L, (byte) (settingsWord));
            } else {
                U.putInt(storageBase, storageAddress + 9L, (int) (settingsWord));
                U.putByte(storageBase, storageAddress + 13L, (byte) (settingsWord >>> 32));
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
        long settingsWord;
        if (bodyBytes == 10) {
            settingsWord = (U.getByte(storageBase, storageAddress + 9L) & 255L);
        } else {
            if (bodyBytes != 14) throw new DecoderException("Invalid packet length");
            settingsWord =
                    ((U.getInt(storageBase, storageAddress + 9L) & 0xFFFFFFFFL)
                            | ((U.getByte(storageBase, storageAddress + 13L) & 255L) << 32));
        }
        long containerIdToZWord = U.getLong(storageBase, storageAddress);
        long zWord = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        long containerIdBits = ((containerIdToZWord >>> 5) & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        int x = (int) (containerIdToZWord >>> 12);
        int z = (int) ((containerIdToZWord >>> 44) | (zWord << 20) | (settingsWord << 28));
        boolean yPresent = ((settingsWord >>> (4)) & 1L) != 0;
        int y = 0;
        if (yPresent) {
            long yBits = ((settingsWord >>> (5)) & 0xFFFFFFFFL);
            y = (int) yBits;
        }
        int settingsBitOffset = yPresent ? 37 : 5;
        if (bodyBytes != (yPresent ? 14 : 10)) throw new DecoderException("Unexpected packet tail");
        if ((settingsWord >>> (settingsBitOffset)) != 0)
            throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)) return;
        ServerActions.satelliteCoordinates(player, containerId, x, z, yPresent, y);
    }
}
