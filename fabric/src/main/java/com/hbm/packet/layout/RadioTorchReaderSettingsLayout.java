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

import com.hbm.blockentity.network.RadioReaderBlockEntity;
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

public final class RadioTorchReaderSettingsLayout {
    public static final int VARIANT = 108;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 18;
    public static final int MAX_BODY_BYTES = 1314;

    public interface Lines {
        String channel(int index);

        String name(int index);
    }

    private RadioTorchReaderSettingsLayout() {}

    public static EncodedPayload encode(int containerId, Lines lines) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        long bodyBits = 16L;
        for (int rowIndex = 0; rowIndex < 8; rowIndex++) {
            {
                int textBytes = PacketValues.utf8Length(lines.channel(rowIndex), 60, 15);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            {
                int textBytes = PacketValues.utf8Length(lines.name(rowIndex), 100, 25);
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
                U.putShort(storageBase, storageAddress, (short) (108L | (containerIdBits << 8)));
            }
            long rowAddress = storageAddress + 2L;
            for (int rowIndex = 0; rowIndex < 8; rowIndex++) {
                rowAddress = PacketValues.putUtf8(storageBase, rowAddress, lines.channel(rowIndex));
                rowAddress = PacketValues.putUtf8(storageBase, rowAddress, lines.name(rowIndex));
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
        int channelBytes = PacketValues.getVarInt(storageBase, storageAddress + 2L, bodyLimit);
        long channelAddress = storageAddress + 2L + PacketValues.varIntSize(channelBytes);
        if ((channelBytes & 0xFFFFFFFFL) > 60 || channelBytes > bodyLimit - channelAddress)
            throw new DecoderException("Invalid text length");
        String channel$0 = PacketValues.getUtf8(storageBase, channelAddress, channelBytes, 15);
        long channelEnd = channelAddress + channelBytes;
        int nameBytes = PacketValues.getVarInt(storageBase, channelEnd, bodyLimit);
        long nameAddress = channelEnd + PacketValues.varIntSize(nameBytes);
        if ((nameBytes & 0xFFFFFFFFL) > 100 || nameBytes > bodyLimit - nameAddress)
            throw new DecoderException("Invalid text length");
        String name$0 = PacketValues.getUtf8(storageBase, nameAddress, nameBytes, 25);
        long nameEnd = nameAddress + nameBytes;
        int channelBytes2 = PacketValues.getVarInt(storageBase, nameEnd, bodyLimit);
        long channelAddress2 = nameEnd + PacketValues.varIntSize(channelBytes2);
        if ((channelBytes2 & 0xFFFFFFFFL) > 60 || channelBytes2 > bodyLimit - channelAddress2)
            throw new DecoderException("Invalid text length");
        String channel$1 = PacketValues.getUtf8(storageBase, channelAddress2, channelBytes2, 15);
        long channelEnd2 = channelAddress2 + channelBytes2;
        int nameBytes2 = PacketValues.getVarInt(storageBase, channelEnd2, bodyLimit);
        long nameAddress2 = channelEnd2 + PacketValues.varIntSize(nameBytes2);
        if ((nameBytes2 & 0xFFFFFFFFL) > 100 || nameBytes2 > bodyLimit - nameAddress2)
            throw new DecoderException("Invalid text length");
        String name$1 = PacketValues.getUtf8(storageBase, nameAddress2, nameBytes2, 25);
        long nameEnd2 = nameAddress2 + nameBytes2;
        int channelBytes3 = PacketValues.getVarInt(storageBase, nameEnd2, bodyLimit);
        long channelAddress3 = nameEnd2 + PacketValues.varIntSize(channelBytes3);
        if ((channelBytes3 & 0xFFFFFFFFL) > 60 || channelBytes3 > bodyLimit - channelAddress3)
            throw new DecoderException("Invalid text length");
        String channel$2 = PacketValues.getUtf8(storageBase, channelAddress3, channelBytes3, 15);
        long channelEnd3 = channelAddress3 + channelBytes3;
        int nameBytes3 = PacketValues.getVarInt(storageBase, channelEnd3, bodyLimit);
        long nameAddress3 = channelEnd3 + PacketValues.varIntSize(nameBytes3);
        if ((nameBytes3 & 0xFFFFFFFFL) > 100 || nameBytes3 > bodyLimit - nameAddress3)
            throw new DecoderException("Invalid text length");
        String name$2 = PacketValues.getUtf8(storageBase, nameAddress3, nameBytes3, 25);
        long nameEnd3 = nameAddress3 + nameBytes3;
        int channelBytes4 = PacketValues.getVarInt(storageBase, nameEnd3, bodyLimit);
        long channelAddress4 = nameEnd3 + PacketValues.varIntSize(channelBytes4);
        if ((channelBytes4 & 0xFFFFFFFFL) > 60 || channelBytes4 > bodyLimit - channelAddress4)
            throw new DecoderException("Invalid text length");
        String channel$3 = PacketValues.getUtf8(storageBase, channelAddress4, channelBytes4, 15);
        long channelEnd4 = channelAddress4 + channelBytes4;
        int nameBytes4 = PacketValues.getVarInt(storageBase, channelEnd4, bodyLimit);
        long nameAddress4 = channelEnd4 + PacketValues.varIntSize(nameBytes4);
        if ((nameBytes4 & 0xFFFFFFFFL) > 100 || nameBytes4 > bodyLimit - nameAddress4)
            throw new DecoderException("Invalid text length");
        String name$3 = PacketValues.getUtf8(storageBase, nameAddress4, nameBytes4, 25);
        long nameEnd4 = nameAddress4 + nameBytes4;
        int channelBytes5 = PacketValues.getVarInt(storageBase, nameEnd4, bodyLimit);
        long channelAddress5 = nameEnd4 + PacketValues.varIntSize(channelBytes5);
        if ((channelBytes5 & 0xFFFFFFFFL) > 60 || channelBytes5 > bodyLimit - channelAddress5)
            throw new DecoderException("Invalid text length");
        String channel$4 = PacketValues.getUtf8(storageBase, channelAddress5, channelBytes5, 15);
        long channelEnd5 = channelAddress5 + channelBytes5;
        int nameBytes5 = PacketValues.getVarInt(storageBase, channelEnd5, bodyLimit);
        long nameAddress5 = channelEnd5 + PacketValues.varIntSize(nameBytes5);
        if ((nameBytes5 & 0xFFFFFFFFL) > 100 || nameBytes5 > bodyLimit - nameAddress5)
            throw new DecoderException("Invalid text length");
        String name$4 = PacketValues.getUtf8(storageBase, nameAddress5, nameBytes5, 25);
        long nameEnd5 = nameAddress5 + nameBytes5;
        int channelBytes6 = PacketValues.getVarInt(storageBase, nameEnd5, bodyLimit);
        long channelAddress6 = nameEnd5 + PacketValues.varIntSize(channelBytes6);
        if ((channelBytes6 & 0xFFFFFFFFL) > 60 || channelBytes6 > bodyLimit - channelAddress6)
            throw new DecoderException("Invalid text length");
        String channel$5 = PacketValues.getUtf8(storageBase, channelAddress6, channelBytes6, 15);
        long channelEnd6 = channelAddress6 + channelBytes6;
        int nameBytes6 = PacketValues.getVarInt(storageBase, channelEnd6, bodyLimit);
        long nameAddress6 = channelEnd6 + PacketValues.varIntSize(nameBytes6);
        if ((nameBytes6 & 0xFFFFFFFFL) > 100 || nameBytes6 > bodyLimit - nameAddress6)
            throw new DecoderException("Invalid text length");
        String name$5 = PacketValues.getUtf8(storageBase, nameAddress6, nameBytes6, 25);
        long nameEnd6 = nameAddress6 + nameBytes6;
        int channelBytes7 = PacketValues.getVarInt(storageBase, nameEnd6, bodyLimit);
        long channelAddress7 = nameEnd6 + PacketValues.varIntSize(channelBytes7);
        if ((channelBytes7 & 0xFFFFFFFFL) > 60 || channelBytes7 > bodyLimit - channelAddress7)
            throw new DecoderException("Invalid text length");
        String channel$6 = PacketValues.getUtf8(storageBase, channelAddress7, channelBytes7, 15);
        long channelEnd7 = channelAddress7 + channelBytes7;
        int nameBytes7 = PacketValues.getVarInt(storageBase, channelEnd7, bodyLimit);
        long nameAddress7 = channelEnd7 + PacketValues.varIntSize(nameBytes7);
        if ((nameBytes7 & 0xFFFFFFFFL) > 100 || nameBytes7 > bodyLimit - nameAddress7)
            throw new DecoderException("Invalid text length");
        String name$6 = PacketValues.getUtf8(storageBase, nameAddress7, nameBytes7, 25);
        long nameEnd7 = nameAddress7 + nameBytes7;
        int channelBytes8 = PacketValues.getVarInt(storageBase, nameEnd7, bodyLimit);
        long channelAddress8 = nameEnd7 + PacketValues.varIntSize(channelBytes8);
        if ((channelBytes8 & 0xFFFFFFFFL) > 60 || channelBytes8 > bodyLimit - channelAddress8)
            throw new DecoderException("Invalid text length");
        String channel$7 = PacketValues.getUtf8(storageBase, channelAddress8, channelBytes8, 15);
        long channelEnd8 = channelAddress8 + channelBytes8;
        int nameBytes8 = PacketValues.getVarInt(storageBase, channelEnd8, bodyLimit);
        long nameAddress8 = channelEnd8 + PacketValues.varIntSize(nameBytes8);
        if ((nameBytes8 & 0xFFFFFFFFL) > 100 || nameBytes8 > bodyLimit - nameAddress8)
            throw new DecoderException("Invalid text length");
        String name$7 = PacketValues.getUtf8(storageBase, nameAddress8, nameBytes8, 25);
        long nameEnd8 = nameAddress8 + nameBytes8;
        if (bodyLimit != nameEnd8) throw new DecoderException("Unexpected packet tail");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RadioReaderBlockEntity receiver)) return;
        {
            int rowIndex = 0;
            String channel = channel$0;
            String name = name$0;
            receiver.setChannel(rowIndex, channel);
            receiver.setName(rowIndex, name);
        }
        {
            int rowIndex = 1;
            String channel = channel$1;
            String name = name$1;
            receiver.setChannel(rowIndex, channel);
            receiver.setName(rowIndex, name);
        }
        {
            int rowIndex = 2;
            String channel = channel$2;
            String name = name$2;
            receiver.setChannel(rowIndex, channel);
            receiver.setName(rowIndex, name);
        }
        {
            int rowIndex = 3;
            String channel = channel$3;
            String name = name$3;
            receiver.setChannel(rowIndex, channel);
            receiver.setName(rowIndex, name);
        }
        {
            int rowIndex = 4;
            String channel = channel$4;
            String name = name$4;
            receiver.setChannel(rowIndex, channel);
            receiver.setName(rowIndex, name);
        }
        {
            int rowIndex = 5;
            String channel = channel$5;
            String name = name$5;
            receiver.setChannel(rowIndex, channel);
            receiver.setName(rowIndex, name);
        }
        {
            int rowIndex = 6;
            String channel = channel$6;
            String name = name$6;
            receiver.setChannel(rowIndex, channel);
            receiver.setName(rowIndex, name);
        }
        {
            int rowIndex = 7;
            String channel = channel$7;
            String name = name$7;
            receiver.setChannel(rowIndex, channel);
            receiver.setName(rowIndex, name);
        }
    }
}
