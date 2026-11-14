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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class PlayerInformLayout {
    public static final int VARIANT = 29;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 10;
    public static final int MAX_BODY_BYTES = 1048576;

    private PlayerInformLayout() {}

    public static EncodedPayload encode(Component message, int id, int millis) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyClientEvents.TYPE, 10, MAX_BODY_BYTES, true, false, false);
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
                long idBits = id;
                long millisBits = millis;
                U.putLong(
                        storageBase,
                        storageAddress,
                        29L | ((idBits & 0xFFFFFFFFL) << 5) | (millisBits << 37));
                U.putByte(storageBase, storageAddress + 8L, (byte) (((millisBits >>> 27) & 31L)));
            }
            encodedBuffer.writerIndex(bufferStart + 9);
            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(codecBuffer, message);
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
        long idToMillisWord = U.getLong(storageBase, storageAddress);
        long millisWord = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        int id = (int) (idToMillisWord >>> 5);
        int millis = (int) ((idToMillisWord >>> 37) | (millisWord << 27));
        if ((millisWord >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        decodedBuffer.readerIndex(bufferStart + 9);
        Component message =
                ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(codecBuffer);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        ClientPacketEvents.playerInform(message, id, millis);
    }
}
