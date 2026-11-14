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

import com.hbm.blockentity.machine.rbmk.RBMKGraphBlockEntity;
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

public final class RBMKGraphSettingsLayout {
    public static final int VARIANT = 14;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 7;
    public static final int MAX_BODY_BYTES = 405;

    public interface Graphs {
        boolean active(int index);

        boolean polling(int index);

        String label(int index);

        String channel(int index);

        boolean minimumPresent(int index);

        long minimum(int index);

        boolean maximumPresent(int index);

        long maximum(int index);
    }

    private RBMKGraphSettingsLayout() {}

    public static EncodedPayload encode(int containerId, Graphs graphs) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        long bodyBits = 16L;
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            bodyBits += 1L;
            bodyBits += 1L;
            bodyBits = (bodyBits + 7L) & ~7L;
            {
                int textBytes = PacketValues.utf8Length(graphs.label(rowIndex), 120, 30);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            {
                int textBytes = PacketValues.utf8Length(graphs.channel(rowIndex), 60, 15);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            bodyBits += 1L;
            if (graphs.minimumPresent(rowIndex)) {
                bodyBits += 64L;
            }
            bodyBits += 1L;
            if (graphs.maximumPresent(rowIndex)) {
                bodyBits += 64L;
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
                U.putShort(storageBase, storageAddress, (short) (14L | (containerIdBits << 8)));
            }
            bitOffset = 16L;
            for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
                {
                    long fieldAddress = storageAddress + (bitOffset >>> 3);
                    int fieldShift = (int) bitOffset & 7;
                    long activeBits = graphs.active(rowIndex) ? 1L : 0L;
                    long pollingBits = graphs.polling(rowIndex) ? 1L : 0L;
                    long fieldWord =
                            partialByte & ((1L << fieldShift) - 1)
                                    | (activeBits | (pollingBits << 1)) << fieldShift;
                    U.putByte(storageBase, fieldAddress, (byte) (fieldWord));
                    if (fieldShift > 6)
                        U.putByte(storageBase, fieldAddress + 1L, (byte) (fieldWord >>> 8));
                }
                bitOffset += 2L;
                bitOffset = (bitOffset + 7L) & ~7L;
                long labelEnd =
                        PacketValues.putUtf8(
                                storageBase,
                                storageAddress + (bitOffset >>> 3),
                                graphs.label(rowIndex));
                long channelEnd =
                        PacketValues.putUtf8(storageBase, labelEnd, graphs.channel(rowIndex));
                {
                    long minimumPresentBits = graphs.minimumPresent(rowIndex) ? 1L : 0L;
                    partialByte = minimumPresentBits;
                    U.putByte(storageBase, channelEnd, (byte) (partialByte));
                }
                if (graphs.minimumPresent(rowIndex)) {
                    {
                        long minimumBits = graphs.minimum(rowIndex);
                        U.putLong(storageBase, channelEnd, partialByte | (minimumBits << 1));
                        partialByte = (minimumBits >>> 63);
                        U.putByte(storageBase, channelEnd + 8L, (byte) (partialByte));
                    }
                }
                long minimumEnd = channelEnd + (graphs.minimumPresent(rowIndex) ? 8L : 0L);
                {
                    long maximumPresentBits = graphs.maximumPresent(rowIndex) ? 1L : 0L;
                    partialByte = partialByte | (maximumPresentBits << 1);
                    U.putByte(storageBase, minimumEnd, (byte) (partialByte));
                }
                if (graphs.maximumPresent(rowIndex)) {
                    {
                        long maximumBits = graphs.maximum(rowIndex);
                        U.putLong(storageBase, minimumEnd, partialByte | (maximumBits << 2));
                        partialByte = (maximumBits >>> 62);
                        U.putByte(storageBase, minimumEnd + 8L, (byte) (partialByte));
                    }
                }
                long maximumEnd = minimumEnd + (graphs.maximumPresent(rowIndex) ? 8L : 0L);
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
        long active$0ToPolling$0Word = (U.getByte(storageBase, storageAddress + 2L) & 255L);
        boolean active$0 = (active$0ToPolling$0Word & 1L) != 0;
        boolean polling$0 = ((active$0ToPolling$0Word >>> 1) & 1L) != 0;
        if ((active$0ToPolling$0Word >>> 2) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes = PacketValues.getVarInt(storageBase, storageAddress + 3L, bodyLimit);
        long labelAddress = storageAddress + 3L + PacketValues.varIntSize(labelBytes);
        if ((labelBytes & 0xFFFFFFFFL) > 120 || labelBytes > bodyLimit - labelAddress)
            throw new DecoderException("Invalid text length");
        String label$0 = PacketValues.getUtf8(storageBase, labelAddress, labelBytes, 30);
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
        long minimum$0 = 0;
        if (minimumPresent$0) {
            if (bodyLimit - channelEnd < 9) throw new DecoderException("Truncated packet fields");
            long minimum$0Word = U.getLong(storageBase, channelEnd);
            long minimum$0WordPart2 = (U.getByte(storageBase, channelEnd + 8L) & 255L);
            minimum$0 = ((minimum$0Word >>> 1) | (minimum$0WordPart2 << 63));
        }
        long minimumEnd = channelEnd + (minimumPresent$0 ? 8L : 0L);
        if (bodyLimit - minimumEnd < 1) throw new DecoderException("Truncated packet fields");
        long maximumPresent$0Word = (U.getByte(storageBase, minimumEnd) & 255L);
        boolean maximumPresent$0 = ((maximumPresent$0Word >>> 1) & 1L) != 0;
        long maximum$0 = 0;
        if (maximumPresent$0) {
            if (bodyLimit - minimumEnd < 9) throw new DecoderException("Truncated packet fields");
            long maximum$0Word = U.getLong(storageBase, minimumEnd);
            long maximum$0WordPart2 = (U.getByte(storageBase, minimumEnd + 8L) & 255L);
            maximum$0 = ((maximum$0Word >>> 2) | (maximum$0WordPart2 << 62));
        }
        long maximumEnd = minimumEnd + (maximumPresent$0 ? 8L : 0L);
        if (bodyLimit - maximumEnd < 1) throw new DecoderException("Truncated packet fields");
        long active$1ToPolling$1Word = (U.getByte(storageBase, maximumEnd) & 255L);
        boolean active$1 = ((active$1ToPolling$1Word >>> 2) & 1L) != 0;
        boolean polling$1 = ((active$1ToPolling$1Word >>> 3) & 1L) != 0;
        if ((active$1ToPolling$1Word >>> 4) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes2 = PacketValues.getVarInt(storageBase, maximumEnd + 1L, bodyLimit);
        long labelAddress2 = maximumEnd + 1L + PacketValues.varIntSize(labelBytes2);
        if ((labelBytes2 & 0xFFFFFFFFL) > 120 || labelBytes2 > bodyLimit - labelAddress2)
            throw new DecoderException("Invalid text length");
        String label$1 = PacketValues.getUtf8(storageBase, labelAddress2, labelBytes2, 30);
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
        long minimum$1 = 0;
        if (minimumPresent$1) {
            if (bodyLimit - channelEnd2 < 9) throw new DecoderException("Truncated packet fields");
            long minimum$1Word = U.getLong(storageBase, channelEnd2);
            long minimum$1WordPart2 = (U.getByte(storageBase, channelEnd2 + 8L) & 255L);
            minimum$1 = ((minimum$1Word >>> 1) | (minimum$1WordPart2 << 63));
        }
        long minimumEnd2 = channelEnd2 + (minimumPresent$1 ? 8L : 0L);
        if (bodyLimit - minimumEnd2 < 1) throw new DecoderException("Truncated packet fields");
        long maximumPresent$1Word = (U.getByte(storageBase, minimumEnd2) & 255L);
        boolean maximumPresent$1 = ((maximumPresent$1Word >>> 1) & 1L) != 0;
        long maximum$1 = 0;
        if (maximumPresent$1) {
            if (bodyLimit - minimumEnd2 < 9) throw new DecoderException("Truncated packet fields");
            long maximum$1Word = U.getLong(storageBase, minimumEnd2);
            long maximum$1WordPart2 = (U.getByte(storageBase, minimumEnd2 + 8L) & 255L);
            maximum$1 = ((maximum$1Word >>> 2) | (maximum$1WordPart2 << 62));
        }
        long maximumEnd2 = minimumEnd2 + (maximumPresent$1 ? 8L : 0L);
        if (bodyLimit != maximumEnd2 + 1L) throw new DecoderException("Unexpected packet tail");
        if (((U.getByte(storageBase, maximumEnd2) & 255) >>> 2) != 0)
            throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RBMKGraphBlockEntity receiver)) return;
        {
            int rowIndex = 0;
            boolean active = active$0;
            boolean polling = polling$0;
            String label = label$0;
            String channel = channel$0;
            boolean minimumPresent = minimumPresent$0;
            long minimum = minimum$0;
            boolean maximumPresent = maximumPresent$0;
            long maximum = maximum$0;
            receiver.configureGraph(
                    rowIndex,
                    active,
                    polling,
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
            String label = label$1;
            String channel = channel$1;
            boolean minimumPresent = minimumPresent$1;
            long minimum = minimum$1;
            boolean maximumPresent = maximumPresent$1;
            long maximum = maximum$1;
            receiver.configureGraph(
                    rowIndex,
                    active,
                    polling,
                    label,
                    channel,
                    minimumPresent,
                    minimum,
                    maximumPresent,
                    maximum);
        }
        receiver.markChanged();
    }
}
