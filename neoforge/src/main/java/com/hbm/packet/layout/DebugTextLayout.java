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

import com.hbm.client.effect.BurstEffects;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

public final class DebugTextLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 34;
    public static final int MAX_BODY_BYTES = 1048576;

    private DebugTextLayout() {}

    public static EncodedPayload encode(
            double x, double y, double z, int color, float scale, String text) {
        int textBytes = PacketValues.utf8Length(text, 2097151, 2097151);
        long bodyBytesWide = 33L + (PacketValues.varIntSize(textBytes) + textBytes);
        if (bodyBytesWide > 1048576) throw new EncoderException("Packet exceeds frame limit");
        int bodyBytes = (int) bodyBytesWide;
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyEffects.TYPE, bodyBytes, MAX_BODY_BYTES, true, false, false);
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
                long xBits = doubleToRawLongBits(x);
                long yBits = doubleToRawLongBits(y);
                long zBits = doubleToRawLongBits(z);
                long colorBits = color;
                long scaleBits = floatToRawIntBits(scale);
                U.putLong(storageBase, storageAddress, (xBits << 5));
                U.putLong(storageBase, storageAddress + 8L, (xBits >>> 59) | (yBits << 5));
                U.putLong(storageBase, storageAddress + 16L, (yBits >>> 59) | (zBits << 5));
                U.putLong(
                        storageBase,
                        storageAddress + 24L,
                        (zBits >>> 59) | ((colorBits & 0xFFFFFFFFL) << 5) | (scaleBits << 37));
                U.putByte(storageBase, storageAddress + 32L, (byte) (((scaleBits >>> 27) & 31L)));
            }
            PacketValues.putUtf8(storageBase, storageAddress + 33L, text, textBytes);
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
        long xWord = U.getLong(storageBase, storageAddress);
        long xToYWord = U.getLong(storageBase, storageAddress + 8L);
        long yToZWord = U.getLong(storageBase, storageAddress + 16L);
        long zToScaleWord = U.getLong(storageBase, storageAddress + 24L);
        long scaleWord = (U.getByte(storageBase, storageAddress + 32L) & 255L);
        double x = longBitsToDouble(((xWord >>> 5) | (xToYWord << 59)));
        double y = longBitsToDouble(((xToYWord >>> 5) | (yToZWord << 59)));
        double z = longBitsToDouble(((yToZWord >>> 5) | (zToScaleWord << 59)));
        int color = (int) (zToScaleWord >>> 5);
        float scale = intBitsToFloat((int) ((zToScaleWord >>> 37) | (scaleWord << 27)));
        if ((scaleWord >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        int textBytes = PacketValues.getVarInt(storageBase, storageAddress + 33L, bodyLimit);
        long textAddress = storageAddress + 33L + PacketValues.varIntSize(textBytes);
        if ((textBytes & 0xFFFFFFFFL) > 2097151 || textBytes > bodyLimit - textAddress)
            throw new DecoderException("Invalid text length");
        String text = PacketValues.getUtf8(storageBase, textAddress, textBytes, 2097151);
        long textEnd = textAddress + textBytes;
        if (bodyLimit != textEnd) throw new DecoderException("Unexpected packet tail");
        BurstEffects.debugText(x, y, z, color, scale, text);
    }
}
