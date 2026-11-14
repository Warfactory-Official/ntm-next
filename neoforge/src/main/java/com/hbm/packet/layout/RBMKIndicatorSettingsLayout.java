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

import com.hbm.blockentity.machine.rbmk.RBMKIndicatorBlockEntity;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.packet.ControlAuthority;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class RBMKIndicatorSettingsLayout {
    public static final int VARIANT = 15;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 35;
    public static final int MAX_BODY_BYTES = 816;

    public interface Indicators {
        boolean active(int index);

        boolean polling(int index);

        int color(int index);

        String label(int index);

        String channel(int index);

        boolean minimumPresent(int index);

        int minimum(int index);

        boolean maximumPresent(int index);

        int maximum(int index);
    }

    private RBMKIndicatorSettingsLayout() {}

    public static EncodedPayload encode(int containerId, Indicators indicators) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        for (int rowIndex = 0; rowIndex < 6; rowIndex++) {
            if (((long) indicators.color(rowIndex) & 0xFFFFFFFFL) > 16777215L)
                throw new IllegalArgumentException("Invalid color");
        }
        long bodyBits = 16L;
        for (int rowIndex = 0; rowIndex < 6; rowIndex++) {
            bodyBits += 1L;
            bodyBits += 1L;
            bodyBits += 24L;
            bodyBits = (bodyBits + 7L) & ~7L;
            {
                int textBytes = PacketValues.utf8Length(indicators.label(rowIndex), 60, 15);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            {
                int textBytes = PacketValues.utf8Length(indicators.channel(rowIndex), 60, 15);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            bodyBits += 1L;
            if (indicators.minimumPresent(rowIndex)) {
                bodyBits += 32L;
            }
            bodyBits += 1L;
            if (indicators.maximumPresent(rowIndex)) {
                bodyBits += 32L;
            }
        }
        int bodyBytes = (int) ((bodyBits + 7L) >>> 3);
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyControls.TYPE, bodyBytes, MAX_BODY_BYTES, false, false, false);
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
            long bitOffset;
            long partialByte = 0L;
            {
                long containerIdBits = ((long) containerId - 1L);
                U.putShort(storageBase, storageAddress, (short) (15L | (containerIdBits << 8)));
            }
            bitOffset = 16L;
            for (int rowIndex = 0; rowIndex < 6; rowIndex++) {
                {
                    long fieldAddress = storageAddress + (bitOffset >>> 3);
                    int fieldShift = (int) bitOffset & 7;
                    long activeBits = indicators.active(rowIndex) ? 1L : 0L;
                    long pollingBits = indicators.polling(rowIndex) ? 1L : 0L;
                    long colorBits = (long) indicators.color(rowIndex);
                    long fieldWord =
                            partialByte & ((1L << fieldShift) - 1)
                                    | (activeBits | (pollingBits << 1) | (colorBits << 2))
                                            << fieldShift;
                    U.putInt(storageBase, fieldAddress, (int) (fieldWord));
                    if (fieldShift > 6)
                        U.putByte(storageBase, fieldAddress + 4L, (byte) (fieldWord >>> 32));
                }
                bitOffset += 26L;
                bitOffset = (bitOffset + 7L) & ~7L;
                long labelEnd =
                        PacketValues.putUtf8(
                                storageBase,
                                storageAddress + (bitOffset >>> 3),
                                indicators.label(rowIndex));
                long channelEnd =
                        PacketValues.putUtf8(storageBase, labelEnd, indicators.channel(rowIndex));
                {
                    long minimumPresentBits = indicators.minimumPresent(rowIndex) ? 1L : 0L;
                    partialByte = minimumPresentBits;
                    U.putByte(storageBase, channelEnd, (byte) (partialByte));
                }
                if (indicators.minimumPresent(rowIndex)) {
                    {
                        long minimumBits = indicators.minimum(rowIndex);
                        U.putInt(storageBase, channelEnd, (int) (partialByte | (minimumBits << 1)));
                        partialByte = ((minimumBits >>> 31) & 1L);
                        U.putByte(storageBase, channelEnd + 4L, (byte) (partialByte));
                    }
                }
                long minimumEnd = channelEnd + (indicators.minimumPresent(rowIndex) ? 4L : 0L);
                {
                    long maximumPresentBits = indicators.maximumPresent(rowIndex) ? 1L : 0L;
                    partialByte = partialByte | (maximumPresentBits << 1);
                    U.putByte(storageBase, minimumEnd, (byte) (partialByte));
                }
                if (indicators.maximumPresent(rowIndex)) {
                    {
                        long maximumBits = indicators.maximum(rowIndex);
                        U.putInt(storageBase, minimumEnd, (int) (partialByte | (maximumBits << 2)));
                        partialByte = ((maximumBits >>> 30) & 3L);
                        U.putByte(storageBase, minimumEnd + 4L, (byte) (partialByte));
                    }
                }
                long maximumEnd = minimumEnd + (indicators.maximumPresent(rowIndex) ? 4L : 0L);
                bitOffset = (maximumEnd - storageAddress) * 8 + 2L;
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
        long containerIdWord = (U.getByte(storageBase, storageAddress + 1L) & 255L);
        long containerIdBits = (containerIdWord & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        if ((containerIdWord >>> 7) != 0) throw new DecoderException("Nonzero packet padding");
        long active$0ToColor$0Word = (U.getInt(storageBase, storageAddress + 2L) & 0xFFFFFFFFL);
        boolean active$0 = (active$0ToColor$0Word & 1L) != 0;
        boolean polling$0 = ((active$0ToColor$0Word >>> 1) & 1L) != 0;
        int color$0 = (int) ((active$0ToColor$0Word >>> 2) & 0xFFFFFFL);
        if ((active$0ToColor$0Word >>> 26) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes = PacketValues.getVarInt(storageBase, storageAddress + 6L, bodyLimit);
        long labelAddress = storageAddress + 6L + PacketValues.varIntSize(labelBytes);
        if ((labelBytes & 0xFFFFFFFFL) > 60 || labelBytes > bodyLimit - labelAddress)
            throw new DecoderException("Invalid text length");
        String label$0 = PacketValues.getUtf8(storageBase, labelAddress, labelBytes, 15);
        long labelEnd = labelAddress + labelBytes;
        int channelBytes = PacketValues.getVarInt(storageBase, labelEnd, bodyLimit);
        long channelAddress = labelEnd + PacketValues.varIntSize(channelBytes);
        if ((channelBytes & 0xFFFFFFFFL) > 60 || channelBytes > bodyLimit - channelAddress)
            throw new DecoderException("Invalid text length");
        String channel$0 = PacketValues.getUtf8(storageBase, channelAddress, channelBytes, 15);
        long channelEnd = channelAddress + channelBytes;
        if (bodyLimit - channelEnd < 1) throw new DecoderException("Truncated packet fields");
        long minimumPresent$0Word = (U.getByte(storageBase, channelEnd) & 255L);
        boolean minimumPresent$0 = (minimumPresent$0Word & 1L) != 0;
        int minimum$0 = 0;
        if (minimumPresent$0) {
            if (bodyLimit - channelEnd < 5) throw new DecoderException("Truncated packet fields");
            long minimum$0Word = (U.getInt(storageBase, channelEnd) & 0xFFFFFFFFL);
            long minimum$0WordPart2 = (U.getByte(storageBase, channelEnd + 4L) & 255L);
            minimum$0 = (int) ((minimum$0Word >>> 1) | (minimum$0WordPart2 << 31));
        }
        long minimumEnd = channelEnd + (minimumPresent$0 ? 4L : 0L);
        if (bodyLimit - minimumEnd < 1) throw new DecoderException("Truncated packet fields");
        long maximumPresent$0Word = (U.getByte(storageBase, minimumEnd) & 255L);
        boolean maximumPresent$0 = ((maximumPresent$0Word >>> 1) & 1L) != 0;
        int maximum$0 = 0;
        if (maximumPresent$0) {
            if (bodyLimit - minimumEnd < 5) throw new DecoderException("Truncated packet fields");
            long maximum$0Word = (U.getInt(storageBase, minimumEnd) & 0xFFFFFFFFL);
            long maximum$0WordPart2 = (U.getByte(storageBase, minimumEnd + 4L) & 255L);
            maximum$0 = (int) ((maximum$0Word >>> 2) | (maximum$0WordPart2 << 30));
        }
        long maximumEnd = minimumEnd + (maximumPresent$0 ? 4L : 0L);
        if (bodyLimit - maximumEnd < 4) throw new DecoderException("Truncated packet fields");
        long active$1ToColor$1Word = (U.getInt(storageBase, maximumEnd) & 0xFFFFFFFFL);
        boolean active$1 = ((active$1ToColor$1Word >>> 2) & 1L) != 0;
        boolean polling$1 = ((active$1ToColor$1Word >>> 3) & 1L) != 0;
        int color$1 = (int) ((active$1ToColor$1Word >>> 4) & 0xFFFFFFL);
        if ((active$1ToColor$1Word >>> 28) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes2 = PacketValues.getVarInt(storageBase, maximumEnd + 4L, bodyLimit);
        long labelAddress2 = maximumEnd + 4L + PacketValues.varIntSize(labelBytes2);
        if ((labelBytes2 & 0xFFFFFFFFL) > 60 || labelBytes2 > bodyLimit - labelAddress2)
            throw new DecoderException("Invalid text length");
        String label$1 = PacketValues.getUtf8(storageBase, labelAddress2, labelBytes2, 15);
        long labelEnd2 = labelAddress2 + labelBytes2;
        int channelBytes2 = PacketValues.getVarInt(storageBase, labelEnd2, bodyLimit);
        long channelAddress2 = labelEnd2 + PacketValues.varIntSize(channelBytes2);
        if ((channelBytes2 & 0xFFFFFFFFL) > 60 || channelBytes2 > bodyLimit - channelAddress2)
            throw new DecoderException("Invalid text length");
        String channel$1 = PacketValues.getUtf8(storageBase, channelAddress2, channelBytes2, 15);
        long channelEnd2 = channelAddress2 + channelBytes2;
        if (bodyLimit - channelEnd2 < 1) throw new DecoderException("Truncated packet fields");
        long minimumPresent$1Word = (U.getByte(storageBase, channelEnd2) & 255L);
        boolean minimumPresent$1 = (minimumPresent$1Word & 1L) != 0;
        int minimum$1 = 0;
        if (minimumPresent$1) {
            if (bodyLimit - channelEnd2 < 5) throw new DecoderException("Truncated packet fields");
            long minimum$1Word = (U.getInt(storageBase, channelEnd2) & 0xFFFFFFFFL);
            long minimum$1WordPart2 = (U.getByte(storageBase, channelEnd2 + 4L) & 255L);
            minimum$1 = (int) ((minimum$1Word >>> 1) | (minimum$1WordPart2 << 31));
        }
        long minimumEnd2 = channelEnd2 + (minimumPresent$1 ? 4L : 0L);
        if (bodyLimit - minimumEnd2 < 1) throw new DecoderException("Truncated packet fields");
        long maximumPresent$1Word = (U.getByte(storageBase, minimumEnd2) & 255L);
        boolean maximumPresent$1 = ((maximumPresent$1Word >>> 1) & 1L) != 0;
        int maximum$1 = 0;
        if (maximumPresent$1) {
            if (bodyLimit - minimumEnd2 < 5) throw new DecoderException("Truncated packet fields");
            long maximum$1Word = (U.getInt(storageBase, minimumEnd2) & 0xFFFFFFFFL);
            long maximum$1WordPart2 = (U.getByte(storageBase, minimumEnd2 + 4L) & 255L);
            maximum$1 = (int) ((maximum$1Word >>> 2) | (maximum$1WordPart2 << 30));
        }
        long maximumEnd2 = minimumEnd2 + (maximumPresent$1 ? 4L : 0L);
        if (bodyLimit - maximumEnd2 < 4) throw new DecoderException("Truncated packet fields");
        long active$2ToColor$2Word = (U.getInt(storageBase, maximumEnd2) & 0xFFFFFFFFL);
        boolean active$2 = ((active$2ToColor$2Word >>> 2) & 1L) != 0;
        boolean polling$2 = ((active$2ToColor$2Word >>> 3) & 1L) != 0;
        int color$2 = (int) ((active$2ToColor$2Word >>> 4) & 0xFFFFFFL);
        if ((active$2ToColor$2Word >>> 28) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes3 = PacketValues.getVarInt(storageBase, maximumEnd2 + 4L, bodyLimit);
        long labelAddress3 = maximumEnd2 + 4L + PacketValues.varIntSize(labelBytes3);
        if ((labelBytes3 & 0xFFFFFFFFL) > 60 || labelBytes3 > bodyLimit - labelAddress3)
            throw new DecoderException("Invalid text length");
        String label$2 = PacketValues.getUtf8(storageBase, labelAddress3, labelBytes3, 15);
        long labelEnd3 = labelAddress3 + labelBytes3;
        int channelBytes3 = PacketValues.getVarInt(storageBase, labelEnd3, bodyLimit);
        long channelAddress3 = labelEnd3 + PacketValues.varIntSize(channelBytes3);
        if ((channelBytes3 & 0xFFFFFFFFL) > 60 || channelBytes3 > bodyLimit - channelAddress3)
            throw new DecoderException("Invalid text length");
        String channel$2 = PacketValues.getUtf8(storageBase, channelAddress3, channelBytes3, 15);
        long channelEnd3 = channelAddress3 + channelBytes3;
        if (bodyLimit - channelEnd3 < 1) throw new DecoderException("Truncated packet fields");
        long minimumPresent$2Word = (U.getByte(storageBase, channelEnd3) & 255L);
        boolean minimumPresent$2 = (minimumPresent$2Word & 1L) != 0;
        int minimum$2 = 0;
        if (minimumPresent$2) {
            if (bodyLimit - channelEnd3 < 5) throw new DecoderException("Truncated packet fields");
            long minimum$2Word = (U.getInt(storageBase, channelEnd3) & 0xFFFFFFFFL);
            long minimum$2WordPart2 = (U.getByte(storageBase, channelEnd3 + 4L) & 255L);
            minimum$2 = (int) ((minimum$2Word >>> 1) | (minimum$2WordPart2 << 31));
        }
        long minimumEnd3 = channelEnd3 + (minimumPresent$2 ? 4L : 0L);
        if (bodyLimit - minimumEnd3 < 1) throw new DecoderException("Truncated packet fields");
        long maximumPresent$2Word = (U.getByte(storageBase, minimumEnd3) & 255L);
        boolean maximumPresent$2 = ((maximumPresent$2Word >>> 1) & 1L) != 0;
        int maximum$2 = 0;
        if (maximumPresent$2) {
            if (bodyLimit - minimumEnd3 < 5) throw new DecoderException("Truncated packet fields");
            long maximum$2Word = (U.getInt(storageBase, minimumEnd3) & 0xFFFFFFFFL);
            long maximum$2WordPart2 = (U.getByte(storageBase, minimumEnd3 + 4L) & 255L);
            maximum$2 = (int) ((maximum$2Word >>> 2) | (maximum$2WordPart2 << 30));
        }
        long maximumEnd3 = minimumEnd3 + (maximumPresent$2 ? 4L : 0L);
        if (bodyLimit - maximumEnd3 < 4) throw new DecoderException("Truncated packet fields");
        long active$3ToColor$3Word = (U.getInt(storageBase, maximumEnd3) & 0xFFFFFFFFL);
        boolean active$3 = ((active$3ToColor$3Word >>> 2) & 1L) != 0;
        boolean polling$3 = ((active$3ToColor$3Word >>> 3) & 1L) != 0;
        int color$3 = (int) ((active$3ToColor$3Word >>> 4) & 0xFFFFFFL);
        if ((active$3ToColor$3Word >>> 28) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes4 = PacketValues.getVarInt(storageBase, maximumEnd3 + 4L, bodyLimit);
        long labelAddress4 = maximumEnd3 + 4L + PacketValues.varIntSize(labelBytes4);
        if ((labelBytes4 & 0xFFFFFFFFL) > 60 || labelBytes4 > bodyLimit - labelAddress4)
            throw new DecoderException("Invalid text length");
        String label$3 = PacketValues.getUtf8(storageBase, labelAddress4, labelBytes4, 15);
        long labelEnd4 = labelAddress4 + labelBytes4;
        int channelBytes4 = PacketValues.getVarInt(storageBase, labelEnd4, bodyLimit);
        long channelAddress4 = labelEnd4 + PacketValues.varIntSize(channelBytes4);
        if ((channelBytes4 & 0xFFFFFFFFL) > 60 || channelBytes4 > bodyLimit - channelAddress4)
            throw new DecoderException("Invalid text length");
        String channel$3 = PacketValues.getUtf8(storageBase, channelAddress4, channelBytes4, 15);
        long channelEnd4 = channelAddress4 + channelBytes4;
        if (bodyLimit - channelEnd4 < 1) throw new DecoderException("Truncated packet fields");
        long minimumPresent$3Word = (U.getByte(storageBase, channelEnd4) & 255L);
        boolean minimumPresent$3 = (minimumPresent$3Word & 1L) != 0;
        int minimum$3 = 0;
        if (minimumPresent$3) {
            if (bodyLimit - channelEnd4 < 5) throw new DecoderException("Truncated packet fields");
            long minimum$3Word = (U.getInt(storageBase, channelEnd4) & 0xFFFFFFFFL);
            long minimum$3WordPart2 = (U.getByte(storageBase, channelEnd4 + 4L) & 255L);
            minimum$3 = (int) ((minimum$3Word >>> 1) | (minimum$3WordPart2 << 31));
        }
        long minimumEnd4 = channelEnd4 + (minimumPresent$3 ? 4L : 0L);
        if (bodyLimit - minimumEnd4 < 1) throw new DecoderException("Truncated packet fields");
        long maximumPresent$3Word = (U.getByte(storageBase, minimumEnd4) & 255L);
        boolean maximumPresent$3 = ((maximumPresent$3Word >>> 1) & 1L) != 0;
        int maximum$3 = 0;
        if (maximumPresent$3) {
            if (bodyLimit - minimumEnd4 < 5) throw new DecoderException("Truncated packet fields");
            long maximum$3Word = (U.getInt(storageBase, minimumEnd4) & 0xFFFFFFFFL);
            long maximum$3WordPart2 = (U.getByte(storageBase, minimumEnd4 + 4L) & 255L);
            maximum$3 = (int) ((maximum$3Word >>> 2) | (maximum$3WordPart2 << 30));
        }
        long maximumEnd4 = minimumEnd4 + (maximumPresent$3 ? 4L : 0L);
        if (bodyLimit - maximumEnd4 < 4) throw new DecoderException("Truncated packet fields");
        long active$4ToColor$4Word = (U.getInt(storageBase, maximumEnd4) & 0xFFFFFFFFL);
        boolean active$4 = ((active$4ToColor$4Word >>> 2) & 1L) != 0;
        boolean polling$4 = ((active$4ToColor$4Word >>> 3) & 1L) != 0;
        int color$4 = (int) ((active$4ToColor$4Word >>> 4) & 0xFFFFFFL);
        if ((active$4ToColor$4Word >>> 28) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes5 = PacketValues.getVarInt(storageBase, maximumEnd4 + 4L, bodyLimit);
        long labelAddress5 = maximumEnd4 + 4L + PacketValues.varIntSize(labelBytes5);
        if ((labelBytes5 & 0xFFFFFFFFL) > 60 || labelBytes5 > bodyLimit - labelAddress5)
            throw new DecoderException("Invalid text length");
        String label$4 = PacketValues.getUtf8(storageBase, labelAddress5, labelBytes5, 15);
        long labelEnd5 = labelAddress5 + labelBytes5;
        int channelBytes5 = PacketValues.getVarInt(storageBase, labelEnd5, bodyLimit);
        long channelAddress5 = labelEnd5 + PacketValues.varIntSize(channelBytes5);
        if ((channelBytes5 & 0xFFFFFFFFL) > 60 || channelBytes5 > bodyLimit - channelAddress5)
            throw new DecoderException("Invalid text length");
        String channel$4 = PacketValues.getUtf8(storageBase, channelAddress5, channelBytes5, 15);
        long channelEnd5 = channelAddress5 + channelBytes5;
        if (bodyLimit - channelEnd5 < 1) throw new DecoderException("Truncated packet fields");
        long minimumPresent$4Word = (U.getByte(storageBase, channelEnd5) & 255L);
        boolean minimumPresent$4 = (minimumPresent$4Word & 1L) != 0;
        int minimum$4 = 0;
        if (minimumPresent$4) {
            if (bodyLimit - channelEnd5 < 5) throw new DecoderException("Truncated packet fields");
            long minimum$4Word = (U.getInt(storageBase, channelEnd5) & 0xFFFFFFFFL);
            long minimum$4WordPart2 = (U.getByte(storageBase, channelEnd5 + 4L) & 255L);
            minimum$4 = (int) ((minimum$4Word >>> 1) | (minimum$4WordPart2 << 31));
        }
        long minimumEnd5 = channelEnd5 + (minimumPresent$4 ? 4L : 0L);
        if (bodyLimit - minimumEnd5 < 1) throw new DecoderException("Truncated packet fields");
        long maximumPresent$4Word = (U.getByte(storageBase, minimumEnd5) & 255L);
        boolean maximumPresent$4 = ((maximumPresent$4Word >>> 1) & 1L) != 0;
        int maximum$4 = 0;
        if (maximumPresent$4) {
            if (bodyLimit - minimumEnd5 < 5) throw new DecoderException("Truncated packet fields");
            long maximum$4Word = (U.getInt(storageBase, minimumEnd5) & 0xFFFFFFFFL);
            long maximum$4WordPart2 = (U.getByte(storageBase, minimumEnd5 + 4L) & 255L);
            maximum$4 = (int) ((maximum$4Word >>> 2) | (maximum$4WordPart2 << 30));
        }
        long maximumEnd5 = minimumEnd5 + (maximumPresent$4 ? 4L : 0L);
        if (bodyLimit - maximumEnd5 < 4) throw new DecoderException("Truncated packet fields");
        long active$5ToColor$5Word = (U.getInt(storageBase, maximumEnd5) & 0xFFFFFFFFL);
        boolean active$5 = ((active$5ToColor$5Word >>> 2) & 1L) != 0;
        boolean polling$5 = ((active$5ToColor$5Word >>> 3) & 1L) != 0;
        int color$5 = (int) ((active$5ToColor$5Word >>> 4) & 0xFFFFFFL);
        if ((active$5ToColor$5Word >>> 28) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes6 = PacketValues.getVarInt(storageBase, maximumEnd5 + 4L, bodyLimit);
        long labelAddress6 = maximumEnd5 + 4L + PacketValues.varIntSize(labelBytes6);
        if ((labelBytes6 & 0xFFFFFFFFL) > 60 || labelBytes6 > bodyLimit - labelAddress6)
            throw new DecoderException("Invalid text length");
        String label$5 = PacketValues.getUtf8(storageBase, labelAddress6, labelBytes6, 15);
        long labelEnd6 = labelAddress6 + labelBytes6;
        int channelBytes6 = PacketValues.getVarInt(storageBase, labelEnd6, bodyLimit);
        long channelAddress6 = labelEnd6 + PacketValues.varIntSize(channelBytes6);
        if ((channelBytes6 & 0xFFFFFFFFL) > 60 || channelBytes6 > bodyLimit - channelAddress6)
            throw new DecoderException("Invalid text length");
        String channel$5 = PacketValues.getUtf8(storageBase, channelAddress6, channelBytes6, 15);
        long channelEnd6 = channelAddress6 + channelBytes6;
        if (bodyLimit - channelEnd6 < 1) throw new DecoderException("Truncated packet fields");
        long minimumPresent$5Word = (U.getByte(storageBase, channelEnd6) & 255L);
        boolean minimumPresent$5 = (minimumPresent$5Word & 1L) != 0;
        int minimum$5 = 0;
        if (minimumPresent$5) {
            if (bodyLimit - channelEnd6 < 5) throw new DecoderException("Truncated packet fields");
            long minimum$5Word = (U.getInt(storageBase, channelEnd6) & 0xFFFFFFFFL);
            long minimum$5WordPart2 = (U.getByte(storageBase, channelEnd6 + 4L) & 255L);
            minimum$5 = (int) ((minimum$5Word >>> 1) | (minimum$5WordPart2 << 31));
        }
        long minimumEnd6 = channelEnd6 + (minimumPresent$5 ? 4L : 0L);
        if (bodyLimit - minimumEnd6 < 1) throw new DecoderException("Truncated packet fields");
        long maximumPresent$5Word = (U.getByte(storageBase, minimumEnd6) & 255L);
        boolean maximumPresent$5 = ((maximumPresent$5Word >>> 1) & 1L) != 0;
        int maximum$5 = 0;
        if (maximumPresent$5) {
            if (bodyLimit - minimumEnd6 < 5) throw new DecoderException("Truncated packet fields");
            long maximum$5Word = (U.getInt(storageBase, minimumEnd6) & 0xFFFFFFFFL);
            long maximum$5WordPart2 = (U.getByte(storageBase, minimumEnd6 + 4L) & 255L);
            maximum$5 = (int) ((maximum$5Word >>> 2) | (maximum$5WordPart2 << 30));
        }
        long maximumEnd6 = minimumEnd6 + (maximumPresent$5 ? 4L : 0L);
        if (bodyLimit != maximumEnd6 + 1L) throw new DecoderException("Unexpected packet tail");
        if (((U.getByte(storageBase, maximumEnd6) & 255) >>> 2) != 0)
            throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RBMKIndicatorBlockEntity receiver)) return;
        {
            int rowIndex = 0;
            boolean active = active$0;
            boolean polling = polling$0;
            int color = color$0;
            String label = label$0;
            String channel = channel$0;
            boolean minimumPresent = minimumPresent$0;
            int minimum = minimum$0;
            boolean maximumPresent = maximumPresent$0;
            int maximum = maximum$0;
            receiver.configureIndicator(
                    rowIndex,
                    active,
                    polling,
                    color,
                    label,
                    channel,
                    minimumPresent,
                    minimum,
                    maximumPresent,
                    maximum);
        }
        {
            int rowIndex = 1;
            boolean active = active$1;
            boolean polling = polling$1;
            int color = color$1;
            String label = label$1;
            String channel = channel$1;
            boolean minimumPresent = minimumPresent$1;
            int minimum = minimum$1;
            boolean maximumPresent = maximumPresent$1;
            int maximum = maximum$1;
            receiver.configureIndicator(
                    rowIndex,
                    active,
                    polling,
                    color,
                    label,
                    channel,
                    minimumPresent,
                    minimum,
                    maximumPresent,
                    maximum);
        }
        {
            int rowIndex = 2;
            boolean active = active$2;
            boolean polling = polling$2;
            int color = color$2;
            String label = label$2;
            String channel = channel$2;
            boolean minimumPresent = minimumPresent$2;
            int minimum = minimum$2;
            boolean maximumPresent = maximumPresent$2;
            int maximum = maximum$2;
            receiver.configureIndicator(
                    rowIndex,
                    active,
                    polling,
                    color,
                    label,
                    channel,
                    minimumPresent,
                    minimum,
                    maximumPresent,
                    maximum);
        }
        {
            int rowIndex = 3;
            boolean active = active$3;
            boolean polling = polling$3;
            int color = color$3;
            String label = label$3;
            String channel = channel$3;
            boolean minimumPresent = minimumPresent$3;
            int minimum = minimum$3;
            boolean maximumPresent = maximumPresent$3;
            int maximum = maximum$3;
            receiver.configureIndicator(
                    rowIndex,
                    active,
                    polling,
                    color,
                    label,
                    channel,
                    minimumPresent,
                    minimum,
                    maximumPresent,
                    maximum);
        }
        {
            int rowIndex = 4;
            boolean active = active$4;
            boolean polling = polling$4;
            int color = color$4;
            String label = label$4;
            String channel = channel$4;
            boolean minimumPresent = minimumPresent$4;
            int minimum = minimum$4;
            boolean maximumPresent = maximumPresent$4;
            int maximum = maximum$4;
            receiver.configureIndicator(
                    rowIndex,
                    active,
                    polling,
                    color,
                    label,
                    channel,
                    minimumPresent,
                    minimum,
                    maximumPresent,
                    maximum);
        }
        {
            int rowIndex = 5;
            boolean active = active$5;
            boolean polling = polling$5;
            int color = color$5;
            String label = label$5;
            String channel = channel$5;
            boolean minimumPresent = minimumPresent$5;
            int minimum = minimum$5;
            boolean maximumPresent = maximumPresent$5;
            int maximum = maximum$5;
            receiver.configureIndicator(
                    rowIndex,
                    active,
                    polling,
                    color,
                    label,
                    channel,
                    minimumPresent,
                    minimum,
                    maximumPresent,
                    maximum);
        }
        receiver.setChanged();
    }
}
