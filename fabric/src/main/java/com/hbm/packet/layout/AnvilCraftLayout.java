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
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class AnvilCraftLayout {
    public static final int VARIANT = 1;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 4;
    public static final int MAX_BODY_BYTES = 32767;

    private AnvilCraftLayout() {}

    public static EncodedPayload encode(int containerId, String recipeName, int mode) {
        int recipeNameBytes = PacketValues.utf8Length(recipeName, 2097151, 2097151);
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        if (((long) mode & 0xFFFFFFFFL) > 1L) throw new IllegalArgumentException("Invalid mode");
        long bodyBytesWide = 3L + (PacketValues.varIntSize(recipeNameBytes) + recipeNameBytes);
        if (bodyBytesWide > 32767) throw new EncoderException("Packet exceeds frame limit");
        int bodyBytes = (int) bodyBytesWide;
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
            {
                long containerIdBits = ((long) containerId - 1L);
                U.putShort(storageBase, storageAddress, (short) (1L | (containerIdBits << 5)));
            }
            long recipeNameEnd =
                    PacketValues.putUtf8(
                            storageBase, storageAddress + 2L, recipeName, recipeNameBytes);
            {
                long modeBits = (long) mode;
                U.putByte(storageBase, recipeNameEnd, (byte) (modeBits));
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
        if (bodyBytes < MIN_BODY_BYTES) throw new DecoderException("Invalid packet length");
        long bodyLimit = storageAddress + bodyBytes;
        long containerIdWord = (U.getShort(storageBase, storageAddress) & 65535L);
        long containerIdBits = ((containerIdWord >>> 5) & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        if ((containerIdWord >>> 12) != 0) throw new DecoderException("Nonzero packet padding");
        int recipeNameBytes = PacketValues.getVarInt(storageBase, storageAddress + 2L, bodyLimit);
        long recipeNameAddress = storageAddress + 2L + PacketValues.varIntSize(recipeNameBytes);
        if ((recipeNameBytes & 0xFFFFFFFFL) > 2097151
                || recipeNameBytes > bodyLimit - recipeNameAddress)
            throw new DecoderException("Invalid text length");
        String recipeName =
                PacketValues.getUtf8(storageBase, recipeNameAddress, recipeNameBytes, 2097151);
        long recipeNameEnd = recipeNameAddress + recipeNameBytes;
        if (bodyLimit - recipeNameEnd != 1)
            throw new DecoderException("Invalid packet tail length");
        long modeWord = (U.getByte(storageBase, recipeNameEnd) & 255L);
        int mode = (int) (modeWord & 1L);
        if ((modeWord >>> 1) != 0) throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)) return;
        ServerActions.anvilCraft(player, containerId, recipeName, mode);
    }
}
