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

import com.hbm.blockentity.machine.rbmk.RBMKNumitronBlockEntity;
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

public final class RBMKNumitronSettingsLayout {
    public static final int VARIANT = 18;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 7;
    public static final int MAX_BODY_BYTES = 373;

    public interface Displays {
        boolean active(int index);

        boolean polling(int index);

        boolean shorten(int index);

        boolean leadingZeroes(int index);

        String label(int index);

        String channel(int index);
    }

    private RBMKNumitronSettingsLayout() {}

    public static EncodedPayload encode(int containerId, Displays displays) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        long bodyBits = 16L;
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            bodyBits += 1L;
            bodyBits += 1L;
            bodyBits += 1L;
            bodyBits += 1L;
            bodyBits = (bodyBits + 7L) & ~7L;
            {
                int textBytes = PacketValues.utf8Length(displays.label(rowIndex), 120, 30);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            {
                int textBytes = PacketValues.utf8Length(displays.channel(rowIndex), 60, 15);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
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
            {
                long containerIdBits = ((long) containerId - 1L);
                U.putShort(storageBase, storageAddress, (short) (18L | (containerIdBits << 8)));
            }
            long rowAddress = storageAddress + 2L;
            for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
                {
                    long activeBits = displays.active(rowIndex) ? 1L : 0L;
                    long pollingBits = displays.polling(rowIndex) ? 1L : 0L;
                    long shortenBits = displays.shorten(rowIndex) ? 1L : 0L;
                    long leadingZeroesBits = displays.leadingZeroes(rowIndex) ? 1L : 0L;
                    U.putByte(
                            storageBase,
                            rowAddress,
                            (byte)
                                    (activeBits
                                            | (pollingBits << 1)
                                            | (shortenBits << 2)
                                            | (leadingZeroesBits << 3)));
                }
                rowAddress =
                        PacketValues.putUtf8(
                                storageBase, rowAddress + 1L, displays.label(rowIndex));
                rowAddress =
                        PacketValues.putUtf8(storageBase, rowAddress, displays.channel(rowIndex));
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
        long active$0ToLeadingZeroes$0Word = (U.getByte(storageBase, storageAddress + 2L) & 255L);
        boolean active$0 = (active$0ToLeadingZeroes$0Word & 1L) != 0;
        boolean polling$0 = ((active$0ToLeadingZeroes$0Word >>> 1) & 1L) != 0;
        boolean shorten$0 = ((active$0ToLeadingZeroes$0Word >>> 2) & 1L) != 0;
        boolean leadingZeroes$0 = ((active$0ToLeadingZeroes$0Word >>> 3) & 1L) != 0;
        if ((active$0ToLeadingZeroes$0Word >>> 4) != 0)
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
        long active$1ToLeadingZeroes$1Word = (U.getByte(storageBase, channelEnd) & 255L);
        boolean active$1 = (active$1ToLeadingZeroes$1Word & 1L) != 0;
        boolean polling$1 = ((active$1ToLeadingZeroes$1Word >>> 1) & 1L) != 0;
        boolean shorten$1 = ((active$1ToLeadingZeroes$1Word >>> 2) & 1L) != 0;
        boolean leadingZeroes$1 = ((active$1ToLeadingZeroes$1Word >>> 3) & 1L) != 0;
        if ((active$1ToLeadingZeroes$1Word >>> 4) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes2 = PacketValues.getVarInt(storageBase, channelEnd + 1L, bodyLimit);
        long labelAddress2 = channelEnd + 1L + PacketValues.varIntSize(labelBytes2);
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
        if (bodyLimit != channelEnd2) throw new DecoderException("Unexpected packet tail");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RBMKNumitronBlockEntity receiver)) return;
        {
            int rowIndex = 0;
            boolean active = active$0;
            boolean polling = polling$0;
            boolean shorten = shorten$0;
            boolean leadingZeroes = leadingZeroes$0;
            String label = label$0;
            String channel = channel$0;
            receiver.configureDisplay(
                    rowIndex, active, polling, shorten, leadingZeroes, label, channel);
        }
        {
            int rowIndex = 1;
            boolean active = active$1;
            boolean polling = polling$1;
            boolean shorten = shorten$1;
            boolean leadingZeroes = leadingZeroes$1;
            String label = label$1;
            String channel = channel$1;
            receiver.configureDisplay(
                    rowIndex, active, polling, shorten, leadingZeroes, label, channel);
        }
        receiver.setChanged();
    }
}
