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

import com.hbm.blockentity.network.RadioTelexBlockEntity;
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

public final class RadioTelexTransmitBufferLayout {
    public static final int VARIANT = 98;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 7;
    public static final int MAX_BODY_BYTES = 679;

    private RadioTelexTransmitBufferLayout() {}

    public static EncodedPayload encode(int containerId, String[] lines) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        if (lines.length != 5) throw new IllegalArgumentException("Invalid lines count");
        long bodyBits = 16L;
        for (int capacityElementIndex = 0; capacityElementIndex < 5; capacityElementIndex++) {
            {
                int textBytes = PacketValues.utf8Length(lines[capacityElementIndex], 132, 33);
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
                U.putShort(storageBase, storageAddress, (short) (98L | (containerIdBits << 8)));
            }
            long elementAddress = storageAddress + 2L;
            for (int elementIndex = 0; elementIndex < 5; elementIndex++) {
                elementAddress =
                        PacketValues.putUtf8(storageBase, elementAddress, lines[elementIndex]);
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
        int lineBytes = PacketValues.getVarInt(storageBase, storageAddress + 2L, bodyLimit);
        long lineAddress = storageAddress + 2L + PacketValues.varIntSize(lineBytes);
        if ((lineBytes & 0xFFFFFFFFL) > 132 || lineBytes > bodyLimit - lineAddress)
            throw new DecoderException("Invalid text length");
        String line$0 = PacketValues.getUtf8(storageBase, lineAddress, lineBytes, 33);
        long lineEnd = lineAddress + lineBytes;
        int lineBytes2 = PacketValues.getVarInt(storageBase, lineEnd, bodyLimit);
        long lineAddress2 = lineEnd + PacketValues.varIntSize(lineBytes2);
        if ((lineBytes2 & 0xFFFFFFFFL) > 132 || lineBytes2 > bodyLimit - lineAddress2)
            throw new DecoderException("Invalid text length");
        String line$1 = PacketValues.getUtf8(storageBase, lineAddress2, lineBytes2, 33);
        long lineEnd2 = lineAddress2 + lineBytes2;
        int lineBytes3 = PacketValues.getVarInt(storageBase, lineEnd2, bodyLimit);
        long lineAddress3 = lineEnd2 + PacketValues.varIntSize(lineBytes3);
        if ((lineBytes3 & 0xFFFFFFFFL) > 132 || lineBytes3 > bodyLimit - lineAddress3)
            throw new DecoderException("Invalid text length");
        String line$2 = PacketValues.getUtf8(storageBase, lineAddress3, lineBytes3, 33);
        long lineEnd3 = lineAddress3 + lineBytes3;
        int lineBytes4 = PacketValues.getVarInt(storageBase, lineEnd3, bodyLimit);
        long lineAddress4 = lineEnd3 + PacketValues.varIntSize(lineBytes4);
        if ((lineBytes4 & 0xFFFFFFFFL) > 132 || lineBytes4 > bodyLimit - lineAddress4)
            throw new DecoderException("Invalid text length");
        String line$3 = PacketValues.getUtf8(storageBase, lineAddress4, lineBytes4, 33);
        long lineEnd4 = lineAddress4 + lineBytes4;
        int lineBytes5 = PacketValues.getVarInt(storageBase, lineEnd4, bodyLimit);
        long lineAddress5 = lineEnd4 + PacketValues.varIntSize(lineBytes5);
        if ((lineBytes5 & 0xFFFFFFFFL) > 132 || lineBytes5 > bodyLimit - lineAddress5)
            throw new DecoderException("Invalid text length");
        String line$4 = PacketValues.getUtf8(storageBase, lineAddress5, lineBytes5, 33);
        long lineEnd5 = lineAddress5 + lineBytes5;
        if (bodyLimit != lineEnd5) throw new DecoderException("Unexpected packet tail");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RadioTelexBlockEntity receiver)) return;
        {
            int elementIndex = 0;
            String line = line$0;
            receiver.setTransmitLine(elementIndex, line);
        }
        {
            int elementIndex = 1;
            String line = line$1;
            receiver.setTransmitLine(elementIndex, line);
        }
        {
            int elementIndex = 2;
            String line = line$2;
            receiver.setTransmitLine(elementIndex, line);
        }
        {
            int elementIndex = 3;
            String line = line$3;
            receiver.setTransmitLine(elementIndex, line);
        }
        {
            int elementIndex = 4;
            String line = line$4;
            receiver.setTransmitLine(elementIndex, line);
        }
        receiver.setChanged();
    }
}
