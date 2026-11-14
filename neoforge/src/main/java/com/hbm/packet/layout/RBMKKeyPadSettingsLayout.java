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

import com.hbm.blockentity.machine.rbmk.RBMKKeyPadBlockEntity;
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

public final class RBMKKeyPadSettingsLayout {
    public static final int VARIANT = 16;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 27;
    public static final int MAX_BODY_BYTES = 1036;

    public interface Keys {
        boolean active(int index);

        boolean polling(int index);

        int color(int index);

        String label(int index);

        String channel(int index);

        String command(int index);
    }

    private RBMKKeyPadSettingsLayout() {}

    public static EncodedPayload encode(int containerId, Keys keys) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        for (int rowIndex = 0; rowIndex < 4; rowIndex++) {
            if (((long) keys.color(rowIndex) & 0xFFFFFFFFL) > 16777215L)
                throw new IllegalArgumentException("Invalid color");
        }
        long bodyBits = 16L;
        for (int rowIndex = 0; rowIndex < 4; rowIndex++) {
            bodyBits += 1L;
            bodyBits += 1L;
            bodyBits += 24L;
            bodyBits = (bodyBits + 7L) & ~7L;
            {
                int textBytes = PacketValues.utf8Length(keys.label(rowIndex), 60, 15);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            {
                int textBytes = PacketValues.utf8Length(keys.channel(rowIndex), 60, 15);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            {
                int textBytes = PacketValues.utf8Length(keys.command(rowIndex), 128, 32);
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
                U.putShort(storageBase, storageAddress, (short) (16L | (containerIdBits << 8)));
            }
            long rowAddress = storageAddress + 2L;
            for (int rowIndex = 0; rowIndex < 4; rowIndex++) {
                {
                    long activeBits = keys.active(rowIndex) ? 1L : 0L;
                    long pollingBits = keys.polling(rowIndex) ? 1L : 0L;
                    long colorBits = (long) keys.color(rowIndex);
                    U.putInt(
                            storageBase,
                            rowAddress,
                            (int) (activeBits | (pollingBits << 1) | (colorBits << 2)));
                }
                rowAddress =
                        PacketValues.putUtf8(storageBase, rowAddress + 4L, keys.label(rowIndex));
                rowAddress = PacketValues.putUtf8(storageBase, rowAddress, keys.channel(rowIndex));
                rowAddress = PacketValues.putUtf8(storageBase, rowAddress, keys.command(rowIndex));
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
        int commandBytes = PacketValues.getVarInt(storageBase, channelEnd, bodyLimit);
        long commandAddress = channelEnd + PacketValues.varIntSize(commandBytes);
        if ((commandBytes & 0xFFFFFFFFL) > 128 || commandBytes > bodyLimit - commandAddress)
            throw new DecoderException("Invalid text length");
        String command$0 = PacketValues.getUtf8(storageBase, commandAddress, commandBytes, 32);
        long commandEnd = commandAddress + commandBytes;
        if (bodyLimit - commandEnd < 4) throw new DecoderException("Truncated packet fields");
        long active$1ToColor$1Word = (U.getInt(storageBase, commandEnd) & 0xFFFFFFFFL);
        boolean active$1 = (active$1ToColor$1Word & 1L) != 0;
        boolean polling$1 = ((active$1ToColor$1Word >>> 1) & 1L) != 0;
        int color$1 = (int) ((active$1ToColor$1Word >>> 2) & 0xFFFFFFL);
        if ((active$1ToColor$1Word >>> 26) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes2 = PacketValues.getVarInt(storageBase, commandEnd + 4L, bodyLimit);
        long labelAddress2 = commandEnd + 4L + PacketValues.varIntSize(labelBytes2);
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
        int commandBytes2 = PacketValues.getVarInt(storageBase, channelEnd2, bodyLimit);
        long commandAddress2 = channelEnd2 + PacketValues.varIntSize(commandBytes2);
        if ((commandBytes2 & 0xFFFFFFFFL) > 128 || commandBytes2 > bodyLimit - commandAddress2)
            throw new DecoderException("Invalid text length");
        String command$1 = PacketValues.getUtf8(storageBase, commandAddress2, commandBytes2, 32);
        long commandEnd2 = commandAddress2 + commandBytes2;
        if (bodyLimit - commandEnd2 < 4) throw new DecoderException("Truncated packet fields");
        long active$2ToColor$2Word = (U.getInt(storageBase, commandEnd2) & 0xFFFFFFFFL);
        boolean active$2 = (active$2ToColor$2Word & 1L) != 0;
        boolean polling$2 = ((active$2ToColor$2Word >>> 1) & 1L) != 0;
        int color$2 = (int) ((active$2ToColor$2Word >>> 2) & 0xFFFFFFL);
        if ((active$2ToColor$2Word >>> 26) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes3 = PacketValues.getVarInt(storageBase, commandEnd2 + 4L, bodyLimit);
        long labelAddress3 = commandEnd2 + 4L + PacketValues.varIntSize(labelBytes3);
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
        int commandBytes3 = PacketValues.getVarInt(storageBase, channelEnd3, bodyLimit);
        long commandAddress3 = channelEnd3 + PacketValues.varIntSize(commandBytes3);
        if ((commandBytes3 & 0xFFFFFFFFL) > 128 || commandBytes3 > bodyLimit - commandAddress3)
            throw new DecoderException("Invalid text length");
        String command$2 = PacketValues.getUtf8(storageBase, commandAddress3, commandBytes3, 32);
        long commandEnd3 = commandAddress3 + commandBytes3;
        if (bodyLimit - commandEnd3 < 4) throw new DecoderException("Truncated packet fields");
        long active$3ToColor$3Word = (U.getInt(storageBase, commandEnd3) & 0xFFFFFFFFL);
        boolean active$3 = (active$3ToColor$3Word & 1L) != 0;
        boolean polling$3 = ((active$3ToColor$3Word >>> 1) & 1L) != 0;
        int color$3 = (int) ((active$3ToColor$3Word >>> 2) & 0xFFFFFFL);
        if ((active$3ToColor$3Word >>> 26) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes4 = PacketValues.getVarInt(storageBase, commandEnd3 + 4L, bodyLimit);
        long labelAddress4 = commandEnd3 + 4L + PacketValues.varIntSize(labelBytes4);
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
        int commandBytes4 = PacketValues.getVarInt(storageBase, channelEnd4, bodyLimit);
        long commandAddress4 = channelEnd4 + PacketValues.varIntSize(commandBytes4);
        if ((commandBytes4 & 0xFFFFFFFFL) > 128 || commandBytes4 > bodyLimit - commandAddress4)
            throw new DecoderException("Invalid text length");
        String command$3 = PacketValues.getUtf8(storageBase, commandAddress4, commandBytes4, 32);
        long commandEnd4 = commandAddress4 + commandBytes4;
        if (bodyLimit != commandEnd4) throw new DecoderException("Unexpected packet tail");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RBMKKeyPadBlockEntity receiver)) return;
        {
            int rowIndex = 0;
            boolean active = active$0;
            boolean polling = polling$0;
            int color = color$0;
            String label = label$0;
            String channel = channel$0;
            String command = command$0;
            receiver.configureKey(rowIndex, active, polling, color, label, channel, command);
        }
        {
            int rowIndex = 1;
            boolean active = active$1;
            boolean polling = polling$1;
            int color = color$1;
            String label = label$1;
            String channel = channel$1;
            String command = command$1;
            receiver.configureKey(rowIndex, active, polling, color, label, channel, command);
        }
        {
            int rowIndex = 2;
            boolean active = active$2;
            boolean polling = polling$2;
            int color = color$2;
            String label = label$2;
            String channel = channel$2;
            String command = command$2;
            receiver.configureKey(rowIndex, active, polling, color, label, channel, command);
        }
        {
            int rowIndex = 3;
            boolean active = active$3;
            boolean polling = polling$3;
            int color = color$3;
            String label = label$3;
            String channel = channel$3;
            String command = command$3;
            receiver.configureKey(rowIndex, active, polling, color, label, channel, command);
        }
        receiver.setChanged();
    }
}
