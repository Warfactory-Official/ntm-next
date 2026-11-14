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

import com.hbm.blockentity.network.RadioTorchReceiverBlockEntity;
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

public final class RadioTorchReceiverSettingsLayout {
    public static final int VARIANT = 104;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 19;
    public static final int MAX_BODY_BYTES = 2160;

    public interface Mapping {
        String entry(int index);
    }

    private RadioTorchReceiverSettingsLayout() {}

    public static EncodedPayload encode(int containerId, String channel, Mapping mapping) {
        int channelBytes = PacketValues.utf8Length(channel, 60, 15);
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        long bodyBits = 16L + 8L * ((PacketValues.varIntSize(channelBytes) + channelBytes));
        for (int elementIndex = 0; elementIndex < 16; elementIndex++) {
            {
                int textBytes = PacketValues.utf8Length(mapping.entry(elementIndex), 128, 32);
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
                U.putShort(storageBase, storageAddress, (short) (104L | (containerIdBits << 8)));
            }
            long channelEnd =
                    PacketValues.putUtf8(storageBase, storageAddress + 2L, channel, channelBytes);
            for (int elementIndex = 0; elementIndex < 16; elementIndex++) {
                channelEnd =
                        PacketValues.putUtf8(storageBase, channelEnd, mapping.entry(elementIndex));
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
        if ((entryBytes & 0xFFFFFFFFL) > 128 || entryBytes > bodyLimit - entryAddress)
            throw new DecoderException("Invalid text length");
        String entry$0 = PacketValues.getUtf8(storageBase, entryAddress, entryBytes, 32);
        long entryEnd = entryAddress + entryBytes;
        int entryBytes2 = PacketValues.getVarInt(storageBase, entryEnd, bodyLimit);
        long entryAddress2 = entryEnd + PacketValues.varIntSize(entryBytes2);
        if ((entryBytes2 & 0xFFFFFFFFL) > 128 || entryBytes2 > bodyLimit - entryAddress2)
            throw new DecoderException("Invalid text length");
        String entry$1 = PacketValues.getUtf8(storageBase, entryAddress2, entryBytes2, 32);
        long entryEnd2 = entryAddress2 + entryBytes2;
        int entryBytes3 = PacketValues.getVarInt(storageBase, entryEnd2, bodyLimit);
        long entryAddress3 = entryEnd2 + PacketValues.varIntSize(entryBytes3);
        if ((entryBytes3 & 0xFFFFFFFFL) > 128 || entryBytes3 > bodyLimit - entryAddress3)
            throw new DecoderException("Invalid text length");
        String entry$2 = PacketValues.getUtf8(storageBase, entryAddress3, entryBytes3, 32);
        long entryEnd3 = entryAddress3 + entryBytes3;
        int entryBytes4 = PacketValues.getVarInt(storageBase, entryEnd3, bodyLimit);
        long entryAddress4 = entryEnd3 + PacketValues.varIntSize(entryBytes4);
        if ((entryBytes4 & 0xFFFFFFFFL) > 128 || entryBytes4 > bodyLimit - entryAddress4)
            throw new DecoderException("Invalid text length");
        String entry$3 = PacketValues.getUtf8(storageBase, entryAddress4, entryBytes4, 32);
        long entryEnd4 = entryAddress4 + entryBytes4;
        int entryBytes5 = PacketValues.getVarInt(storageBase, entryEnd4, bodyLimit);
        long entryAddress5 = entryEnd4 + PacketValues.varIntSize(entryBytes5);
        if ((entryBytes5 & 0xFFFFFFFFL) > 128 || entryBytes5 > bodyLimit - entryAddress5)
            throw new DecoderException("Invalid text length");
        String entry$4 = PacketValues.getUtf8(storageBase, entryAddress5, entryBytes5, 32);
        long entryEnd5 = entryAddress5 + entryBytes5;
        int entryBytes6 = PacketValues.getVarInt(storageBase, entryEnd5, bodyLimit);
        long entryAddress6 = entryEnd5 + PacketValues.varIntSize(entryBytes6);
        if ((entryBytes6 & 0xFFFFFFFFL) > 128 || entryBytes6 > bodyLimit - entryAddress6)
            throw new DecoderException("Invalid text length");
        String entry$5 = PacketValues.getUtf8(storageBase, entryAddress6, entryBytes6, 32);
        long entryEnd6 = entryAddress6 + entryBytes6;
        int entryBytes7 = PacketValues.getVarInt(storageBase, entryEnd6, bodyLimit);
        long entryAddress7 = entryEnd6 + PacketValues.varIntSize(entryBytes7);
        if ((entryBytes7 & 0xFFFFFFFFL) > 128 || entryBytes7 > bodyLimit - entryAddress7)
            throw new DecoderException("Invalid text length");
        String entry$6 = PacketValues.getUtf8(storageBase, entryAddress7, entryBytes7, 32);
        long entryEnd7 = entryAddress7 + entryBytes7;
        int entryBytes8 = PacketValues.getVarInt(storageBase, entryEnd7, bodyLimit);
        long entryAddress8 = entryEnd7 + PacketValues.varIntSize(entryBytes8);
        if ((entryBytes8 & 0xFFFFFFFFL) > 128 || entryBytes8 > bodyLimit - entryAddress8)
            throw new DecoderException("Invalid text length");
        String entry$7 = PacketValues.getUtf8(storageBase, entryAddress8, entryBytes8, 32);
        long entryEnd8 = entryAddress8 + entryBytes8;
        int entryBytes9 = PacketValues.getVarInt(storageBase, entryEnd8, bodyLimit);
        long entryAddress9 = entryEnd8 + PacketValues.varIntSize(entryBytes9);
        if ((entryBytes9 & 0xFFFFFFFFL) > 128 || entryBytes9 > bodyLimit - entryAddress9)
            throw new DecoderException("Invalid text length");
        String entry$8 = PacketValues.getUtf8(storageBase, entryAddress9, entryBytes9, 32);
        long entryEnd9 = entryAddress9 + entryBytes9;
        int entryBytes10 = PacketValues.getVarInt(storageBase, entryEnd9, bodyLimit);
        long entryAddress10 = entryEnd9 + PacketValues.varIntSize(entryBytes10);
        if ((entryBytes10 & 0xFFFFFFFFL) > 128 || entryBytes10 > bodyLimit - entryAddress10)
            throw new DecoderException("Invalid text length");
        String entry$9 = PacketValues.getUtf8(storageBase, entryAddress10, entryBytes10, 32);
        long entryEnd10 = entryAddress10 + entryBytes10;
        int entryBytes11 = PacketValues.getVarInt(storageBase, entryEnd10, bodyLimit);
        long entryAddress11 = entryEnd10 + PacketValues.varIntSize(entryBytes11);
        if ((entryBytes11 & 0xFFFFFFFFL) > 128 || entryBytes11 > bodyLimit - entryAddress11)
            throw new DecoderException("Invalid text length");
        String entry$10 = PacketValues.getUtf8(storageBase, entryAddress11, entryBytes11, 32);
        long entryEnd11 = entryAddress11 + entryBytes11;
        int entryBytes12 = PacketValues.getVarInt(storageBase, entryEnd11, bodyLimit);
        long entryAddress12 = entryEnd11 + PacketValues.varIntSize(entryBytes12);
        if ((entryBytes12 & 0xFFFFFFFFL) > 128 || entryBytes12 > bodyLimit - entryAddress12)
            throw new DecoderException("Invalid text length");
        String entry$11 = PacketValues.getUtf8(storageBase, entryAddress12, entryBytes12, 32);
        long entryEnd12 = entryAddress12 + entryBytes12;
        int entryBytes13 = PacketValues.getVarInt(storageBase, entryEnd12, bodyLimit);
        long entryAddress13 = entryEnd12 + PacketValues.varIntSize(entryBytes13);
        if ((entryBytes13 & 0xFFFFFFFFL) > 128 || entryBytes13 > bodyLimit - entryAddress13)
            throw new DecoderException("Invalid text length");
        String entry$12 = PacketValues.getUtf8(storageBase, entryAddress13, entryBytes13, 32);
        long entryEnd13 = entryAddress13 + entryBytes13;
        int entryBytes14 = PacketValues.getVarInt(storageBase, entryEnd13, bodyLimit);
        long entryAddress14 = entryEnd13 + PacketValues.varIntSize(entryBytes14);
        if ((entryBytes14 & 0xFFFFFFFFL) > 128 || entryBytes14 > bodyLimit - entryAddress14)
            throw new DecoderException("Invalid text length");
        String entry$13 = PacketValues.getUtf8(storageBase, entryAddress14, entryBytes14, 32);
        long entryEnd14 = entryAddress14 + entryBytes14;
        int entryBytes15 = PacketValues.getVarInt(storageBase, entryEnd14, bodyLimit);
        long entryAddress15 = entryEnd14 + PacketValues.varIntSize(entryBytes15);
        if ((entryBytes15 & 0xFFFFFFFFL) > 128 || entryBytes15 > bodyLimit - entryAddress15)
            throw new DecoderException("Invalid text length");
        String entry$14 = PacketValues.getUtf8(storageBase, entryAddress15, entryBytes15, 32);
        long entryEnd15 = entryAddress15 + entryBytes15;
        int entryBytes16 = PacketValues.getVarInt(storageBase, entryEnd15, bodyLimit);
        long entryAddress16 = entryEnd15 + PacketValues.varIntSize(entryBytes16);
        if ((entryBytes16 & 0xFFFFFFFFL) > 128 || entryBytes16 > bodyLimit - entryAddress16)
            throw new DecoderException("Invalid text length");
        String entry$15 = PacketValues.getUtf8(storageBase, entryAddress16, entryBytes16, 32);
        long entryEnd16 = entryAddress16 + entryBytes16;
        if (bodyLimit != entryEnd16) throw new DecoderException("Unexpected packet tail");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RadioTorchReceiverBlockEntity receiver)) return;
        {
            int elementIndex = 0;
            String entry = entry$0;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 1;
            String entry = entry$1;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 2;
            String entry = entry$2;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 3;
            String entry = entry$3;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 4;
            String entry = entry$4;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 5;
            String entry = entry$5;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 6;
            String entry = entry$6;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 7;
            String entry = entry$7;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 8;
            String entry = entry$8;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 9;
            String entry = entry$9;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 10;
            String entry = entry$10;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 11;
            String entry = entry$11;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 12;
            String entry = entry$12;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 13;
            String entry = entry$13;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 14;
            String entry = entry$14;
            receiver.setMapping(elementIndex, entry);
        }
        {
            int elementIndex = 15;
            String entry = entry$15;
            receiver.setMapping(elementIndex, entry);
        }
        receiver.setMappedChannel(channel);
    }
}
