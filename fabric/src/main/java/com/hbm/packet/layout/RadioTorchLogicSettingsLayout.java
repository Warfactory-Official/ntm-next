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

import com.hbm.blockentity.network.RadioLogicBlockEntity;
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

public final class RadioTorchLogicSettingsLayout {
    public static final int VARIANT = 105;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 83;
    public static final int MAX_BODY_BYTES = 1120;

    public interface Entries {
        String entry(int index);

        int condition(int index);
    }

    private RadioTorchLogicSettingsLayout() {}

    public static EncodedPayload encode(int containerId, String channel, Entries entries) {
        int channelBytes = PacketValues.utf8Length(channel, 60, 15);
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        long bodyBits = 16L + 8L * ((PacketValues.varIntSize(channelBytes) + channelBytes));
        for (int rowIndex = 0; rowIndex < 16; rowIndex++) {
            {
                int textBytes = PacketValues.utf8Length(entries.entry(rowIndex), 60, 15);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            bodyBits += 32L;
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
                U.putShort(storageBase, storageAddress, (short) (105L | (containerIdBits << 8)));
            }
            long channelEnd =
                    PacketValues.putUtf8(storageBase, storageAddress + 2L, channel, channelBytes);
            for (int rowIndex = 0; rowIndex < 16; rowIndex++) {
                channelEnd = PacketValues.putUtf8(storageBase, channelEnd, entries.entry(rowIndex));
                {
                    long conditionBits = entries.condition(rowIndex);
                    U.putInt(storageBase, channelEnd, (int) (conditionBits));
                }
                channelEnd += 4L;
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
        String channel = PacketValues.getUtf8(storageBase, channelAddress, channelBytes, 15);
        long channelEnd = channelAddress + channelBytes;
        int entryBytes = PacketValues.getVarInt(storageBase, channelEnd, bodyLimit);
        long entryAddress = channelEnd + PacketValues.varIntSize(entryBytes);
        if ((entryBytes & 0xFFFFFFFFL) > 60 || entryBytes > bodyLimit - entryAddress)
            throw new DecoderException("Invalid text length");
        String entry$0 = PacketValues.getUtf8(storageBase, entryAddress, entryBytes, 15);
        long entryEnd = entryAddress + entryBytes;
        if (bodyLimit - entryEnd < 4) throw new DecoderException("Truncated packet fields");
        long condition$0Word = (U.getInt(storageBase, entryEnd) & 0xFFFFFFFFL);
        int condition$0 = (int) condition$0Word;
        int entryBytes2 = PacketValues.getVarInt(storageBase, entryEnd + 4L, bodyLimit);
        long entryAddress2 = entryEnd + 4L + PacketValues.varIntSize(entryBytes2);
        if ((entryBytes2 & 0xFFFFFFFFL) > 60 || entryBytes2 > bodyLimit - entryAddress2)
            throw new DecoderException("Invalid text length");
        String entry$1 = PacketValues.getUtf8(storageBase, entryAddress2, entryBytes2, 15);
        long entryEnd2 = entryAddress2 + entryBytes2;
        if (bodyLimit - entryEnd2 < 4) throw new DecoderException("Truncated packet fields");
        long condition$1Word = (U.getInt(storageBase, entryEnd2) & 0xFFFFFFFFL);
        int condition$1 = (int) condition$1Word;
        int entryBytes3 = PacketValues.getVarInt(storageBase, entryEnd2 + 4L, bodyLimit);
        long entryAddress3 = entryEnd2 + 4L + PacketValues.varIntSize(entryBytes3);
        if ((entryBytes3 & 0xFFFFFFFFL) > 60 || entryBytes3 > bodyLimit - entryAddress3)
            throw new DecoderException("Invalid text length");
        String entry$2 = PacketValues.getUtf8(storageBase, entryAddress3, entryBytes3, 15);
        long entryEnd3 = entryAddress3 + entryBytes3;
        if (bodyLimit - entryEnd3 < 4) throw new DecoderException("Truncated packet fields");
        long condition$2Word = (U.getInt(storageBase, entryEnd3) & 0xFFFFFFFFL);
        int condition$2 = (int) condition$2Word;
        int entryBytes4 = PacketValues.getVarInt(storageBase, entryEnd3 + 4L, bodyLimit);
        long entryAddress4 = entryEnd3 + 4L + PacketValues.varIntSize(entryBytes4);
        if ((entryBytes4 & 0xFFFFFFFFL) > 60 || entryBytes4 > bodyLimit - entryAddress4)
            throw new DecoderException("Invalid text length");
        String entry$3 = PacketValues.getUtf8(storageBase, entryAddress4, entryBytes4, 15);
        long entryEnd4 = entryAddress4 + entryBytes4;
        if (bodyLimit - entryEnd4 < 4) throw new DecoderException("Truncated packet fields");
        long condition$3Word = (U.getInt(storageBase, entryEnd4) & 0xFFFFFFFFL);
        int condition$3 = (int) condition$3Word;
        int entryBytes5 = PacketValues.getVarInt(storageBase, entryEnd4 + 4L, bodyLimit);
        long entryAddress5 = entryEnd4 + 4L + PacketValues.varIntSize(entryBytes5);
        if ((entryBytes5 & 0xFFFFFFFFL) > 60 || entryBytes5 > bodyLimit - entryAddress5)
            throw new DecoderException("Invalid text length");
        String entry$4 = PacketValues.getUtf8(storageBase, entryAddress5, entryBytes5, 15);
        long entryEnd5 = entryAddress5 + entryBytes5;
        if (bodyLimit - entryEnd5 < 4) throw new DecoderException("Truncated packet fields");
        long condition$4Word = (U.getInt(storageBase, entryEnd5) & 0xFFFFFFFFL);
        int condition$4 = (int) condition$4Word;
        int entryBytes6 = PacketValues.getVarInt(storageBase, entryEnd5 + 4L, bodyLimit);
        long entryAddress6 = entryEnd5 + 4L + PacketValues.varIntSize(entryBytes6);
        if ((entryBytes6 & 0xFFFFFFFFL) > 60 || entryBytes6 > bodyLimit - entryAddress6)
            throw new DecoderException("Invalid text length");
        String entry$5 = PacketValues.getUtf8(storageBase, entryAddress6, entryBytes6, 15);
        long entryEnd6 = entryAddress6 + entryBytes6;
        if (bodyLimit - entryEnd6 < 4) throw new DecoderException("Truncated packet fields");
        long condition$5Word = (U.getInt(storageBase, entryEnd6) & 0xFFFFFFFFL);
        int condition$5 = (int) condition$5Word;
        int entryBytes7 = PacketValues.getVarInt(storageBase, entryEnd6 + 4L, bodyLimit);
        long entryAddress7 = entryEnd6 + 4L + PacketValues.varIntSize(entryBytes7);
        if ((entryBytes7 & 0xFFFFFFFFL) > 60 || entryBytes7 > bodyLimit - entryAddress7)
            throw new DecoderException("Invalid text length");
        String entry$6 = PacketValues.getUtf8(storageBase, entryAddress7, entryBytes7, 15);
        long entryEnd7 = entryAddress7 + entryBytes7;
        if (bodyLimit - entryEnd7 < 4) throw new DecoderException("Truncated packet fields");
        long condition$6Word = (U.getInt(storageBase, entryEnd7) & 0xFFFFFFFFL);
        int condition$6 = (int) condition$6Word;
        int entryBytes8 = PacketValues.getVarInt(storageBase, entryEnd7 + 4L, bodyLimit);
        long entryAddress8 = entryEnd7 + 4L + PacketValues.varIntSize(entryBytes8);
        if ((entryBytes8 & 0xFFFFFFFFL) > 60 || entryBytes8 > bodyLimit - entryAddress8)
            throw new DecoderException("Invalid text length");
        String entry$7 = PacketValues.getUtf8(storageBase, entryAddress8, entryBytes8, 15);
        long entryEnd8 = entryAddress8 + entryBytes8;
        if (bodyLimit - entryEnd8 < 4) throw new DecoderException("Truncated packet fields");
        long condition$7Word = (U.getInt(storageBase, entryEnd8) & 0xFFFFFFFFL);
        int condition$7 = (int) condition$7Word;
        int entryBytes9 = PacketValues.getVarInt(storageBase, entryEnd8 + 4L, bodyLimit);
        long entryAddress9 = entryEnd8 + 4L + PacketValues.varIntSize(entryBytes9);
        if ((entryBytes9 & 0xFFFFFFFFL) > 60 || entryBytes9 > bodyLimit - entryAddress9)
            throw new DecoderException("Invalid text length");
        String entry$8 = PacketValues.getUtf8(storageBase, entryAddress9, entryBytes9, 15);
        long entryEnd9 = entryAddress9 + entryBytes9;
        if (bodyLimit - entryEnd9 < 4) throw new DecoderException("Truncated packet fields");
        long condition$8Word = (U.getInt(storageBase, entryEnd9) & 0xFFFFFFFFL);
        int condition$8 = (int) condition$8Word;
        int entryBytes10 = PacketValues.getVarInt(storageBase, entryEnd9 + 4L, bodyLimit);
        long entryAddress10 = entryEnd9 + 4L + PacketValues.varIntSize(entryBytes10);
        if ((entryBytes10 & 0xFFFFFFFFL) > 60 || entryBytes10 > bodyLimit - entryAddress10)
            throw new DecoderException("Invalid text length");
        String entry$9 = PacketValues.getUtf8(storageBase, entryAddress10, entryBytes10, 15);
        long entryEnd10 = entryAddress10 + entryBytes10;
        if (bodyLimit - entryEnd10 < 4) throw new DecoderException("Truncated packet fields");
        long condition$9Word = (U.getInt(storageBase, entryEnd10) & 0xFFFFFFFFL);
        int condition$9 = (int) condition$9Word;
        int entryBytes11 = PacketValues.getVarInt(storageBase, entryEnd10 + 4L, bodyLimit);
        long entryAddress11 = entryEnd10 + 4L + PacketValues.varIntSize(entryBytes11);
        if ((entryBytes11 & 0xFFFFFFFFL) > 60 || entryBytes11 > bodyLimit - entryAddress11)
            throw new DecoderException("Invalid text length");
        String entry$10 = PacketValues.getUtf8(storageBase, entryAddress11, entryBytes11, 15);
        long entryEnd11 = entryAddress11 + entryBytes11;
        if (bodyLimit - entryEnd11 < 4) throw new DecoderException("Truncated packet fields");
        long condition$10Word = (U.getInt(storageBase, entryEnd11) & 0xFFFFFFFFL);
        int condition$10 = (int) condition$10Word;
        int entryBytes12 = PacketValues.getVarInt(storageBase, entryEnd11 + 4L, bodyLimit);
        long entryAddress12 = entryEnd11 + 4L + PacketValues.varIntSize(entryBytes12);
        if ((entryBytes12 & 0xFFFFFFFFL) > 60 || entryBytes12 > bodyLimit - entryAddress12)
            throw new DecoderException("Invalid text length");
        String entry$11 = PacketValues.getUtf8(storageBase, entryAddress12, entryBytes12, 15);
        long entryEnd12 = entryAddress12 + entryBytes12;
        if (bodyLimit - entryEnd12 < 4) throw new DecoderException("Truncated packet fields");
        long condition$11Word = (U.getInt(storageBase, entryEnd12) & 0xFFFFFFFFL);
        int condition$11 = (int) condition$11Word;
        int entryBytes13 = PacketValues.getVarInt(storageBase, entryEnd12 + 4L, bodyLimit);
        long entryAddress13 = entryEnd12 + 4L + PacketValues.varIntSize(entryBytes13);
        if ((entryBytes13 & 0xFFFFFFFFL) > 60 || entryBytes13 > bodyLimit - entryAddress13)
            throw new DecoderException("Invalid text length");
        String entry$12 = PacketValues.getUtf8(storageBase, entryAddress13, entryBytes13, 15);
        long entryEnd13 = entryAddress13 + entryBytes13;
        if (bodyLimit - entryEnd13 < 4) throw new DecoderException("Truncated packet fields");
        long condition$12Word = (U.getInt(storageBase, entryEnd13) & 0xFFFFFFFFL);
        int condition$12 = (int) condition$12Word;
        int entryBytes14 = PacketValues.getVarInt(storageBase, entryEnd13 + 4L, bodyLimit);
        long entryAddress14 = entryEnd13 + 4L + PacketValues.varIntSize(entryBytes14);
        if ((entryBytes14 & 0xFFFFFFFFL) > 60 || entryBytes14 > bodyLimit - entryAddress14)
            throw new DecoderException("Invalid text length");
        String entry$13 = PacketValues.getUtf8(storageBase, entryAddress14, entryBytes14, 15);
        long entryEnd14 = entryAddress14 + entryBytes14;
        if (bodyLimit - entryEnd14 < 4) throw new DecoderException("Truncated packet fields");
        long condition$13Word = (U.getInt(storageBase, entryEnd14) & 0xFFFFFFFFL);
        int condition$13 = (int) condition$13Word;
        int entryBytes15 = PacketValues.getVarInt(storageBase, entryEnd14 + 4L, bodyLimit);
        long entryAddress15 = entryEnd14 + 4L + PacketValues.varIntSize(entryBytes15);
        if ((entryBytes15 & 0xFFFFFFFFL) > 60 || entryBytes15 > bodyLimit - entryAddress15)
            throw new DecoderException("Invalid text length");
        String entry$14 = PacketValues.getUtf8(storageBase, entryAddress15, entryBytes15, 15);
        long entryEnd15 = entryAddress15 + entryBytes15;
        if (bodyLimit - entryEnd15 < 4) throw new DecoderException("Truncated packet fields");
        long condition$14Word = (U.getInt(storageBase, entryEnd15) & 0xFFFFFFFFL);
        int condition$14 = (int) condition$14Word;
        int entryBytes16 = PacketValues.getVarInt(storageBase, entryEnd15 + 4L, bodyLimit);
        long entryAddress16 = entryEnd15 + 4L + PacketValues.varIntSize(entryBytes16);
        if ((entryBytes16 & 0xFFFFFFFFL) > 60 || entryBytes16 > bodyLimit - entryAddress16)
            throw new DecoderException("Invalid text length");
        String entry$15 = PacketValues.getUtf8(storageBase, entryAddress16, entryBytes16, 15);
        long entryEnd16 = entryAddress16 + entryBytes16;
        if (bodyLimit - entryEnd16 != 4) throw new DecoderException("Invalid packet tail length");
        long condition$15Word = (U.getInt(storageBase, entryEnd16) & 0xFFFFFFFFL);
        int condition$15 = (int) condition$15Word;
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RadioLogicBlockEntity receiver)) return;
        {
            int rowIndex = 0;
            String entry = entry$0;
            int condition = condition$0;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 1;
            String entry = entry$1;
            int condition = condition$1;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 2;
            String entry = entry$2;
            int condition = condition$2;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 3;
            String entry = entry$3;
            int condition = condition$3;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 4;
            String entry = entry$4;
            int condition = condition$4;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 5;
            String entry = entry$5;
            int condition = condition$5;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 6;
            String entry = entry$6;
            int condition = condition$6;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 7;
            String entry = entry$7;
            int condition = condition$7;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 8;
            String entry = entry$8;
            int condition = condition$8;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 9;
            String entry = entry$9;
            int condition = condition$9;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 10;
            String entry = entry$10;
            int condition = condition$10;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 11;
            String entry = entry$11;
            int condition = condition$11;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 12;
            String entry = entry$12;
            int condition = condition$12;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 13;
            String entry = entry$13;
            int condition = condition$13;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 14;
            String entry = entry$14;
            int condition = condition$14;
            receiver.setEntry(rowIndex, entry, condition);
        }
        {
            int rowIndex = 15;
            String entry = entry$15;
            int condition = condition$15;
            receiver.setEntry(rowIndex, entry, condition);
        }
        receiver.setMappedChannel(channel);
    }
}
