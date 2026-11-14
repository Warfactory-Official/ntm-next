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

import com.hbm.blockentity.machine.rbmk.RBMKLeverBlockEntity;
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

public final class RBMKLeverSettingsLayout {
    public static final int VARIANT = 17;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 11;
    public static final int MAX_BODY_BYTES = 776;

    public interface Levers {
        boolean active(int index);

        boolean polling(int index);

        String label(int index);

        String channel(int index);

        String commandOn(int index);

        String commandOff(int index);
    }

    private RBMKLeverSettingsLayout() {}

    public static EncodedPayload encode(int containerId, Levers levers) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        long bodyBits = 16L;
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            bodyBits += 1L;
            bodyBits += 1L;
            bodyBits = (bodyBits + 7L) & ~7L;
            {
                int textBytes = PacketValues.utf8Length(levers.label(rowIndex), 60, 15);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            {
                int textBytes = PacketValues.utf8Length(levers.channel(rowIndex), 60, 15);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            {
                int textBytes = PacketValues.utf8Length(levers.commandOn(rowIndex), 128, 32);
                bodyBits += 8L * (PacketValues.varIntSize(textBytes) + textBytes);
            }
            {
                int textBytes = PacketValues.utf8Length(levers.commandOff(rowIndex), 128, 32);
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
                U.putShort(storageBase, storageAddress, (short) (17L | (containerIdBits << 8)));
            }
            long rowAddress = storageAddress + 2L;
            for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
                {
                    long activeBits = levers.active(rowIndex) ? 1L : 0L;
                    long pollingBits = levers.polling(rowIndex) ? 1L : 0L;
                    U.putByte(storageBase, rowAddress, (byte) (activeBits | (pollingBits << 1)));
                }
                rowAddress =
                        PacketValues.putUtf8(storageBase, rowAddress + 1L, levers.label(rowIndex));
                rowAddress =
                        PacketValues.putUtf8(storageBase, rowAddress, levers.channel(rowIndex));
                rowAddress =
                        PacketValues.putUtf8(storageBase, rowAddress, levers.commandOn(rowIndex));
                rowAddress =
                        PacketValues.putUtf8(storageBase, rowAddress, levers.commandOff(rowIndex));
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
        int commandOnBytes = PacketValues.getVarInt(storageBase, channelEnd, bodyLimit);
        long commandOnAddress = channelEnd + PacketValues.varIntSize(commandOnBytes);
        if ((commandOnBytes & 0xFFFFFFFFL) > 128 || commandOnBytes > bodyLimit - commandOnAddress)
            throw new DecoderException("Invalid text length");
        String commandOn$0 =
                PacketValues.getUtf8(storageBase, commandOnAddress, commandOnBytes, 32);
        long commandOnEnd = commandOnAddress + commandOnBytes;
        int commandOffBytes = PacketValues.getVarInt(storageBase, commandOnEnd, bodyLimit);
        long commandOffAddress = commandOnEnd + PacketValues.varIntSize(commandOffBytes);
        if ((commandOffBytes & 0xFFFFFFFFL) > 128
                || commandOffBytes > bodyLimit - commandOffAddress)
            throw new DecoderException("Invalid text length");
        String commandOff$0 =
                PacketValues.getUtf8(storageBase, commandOffAddress, commandOffBytes, 32);
        long commandOffEnd = commandOffAddress + commandOffBytes;
        if (bodyLimit - commandOffEnd < 1) throw new DecoderException("Truncated packet fields");
        long active$1ToPolling$1Word = (U.getByte(storageBase, commandOffEnd) & 255L);
        boolean active$1 = (active$1ToPolling$1Word & 1L) != 0;
        boolean polling$1 = ((active$1ToPolling$1Word >>> 1) & 1L) != 0;
        if ((active$1ToPolling$1Word >>> 2) != 0)
            throw new DecoderException("Nonzero packet padding");
        int labelBytes2 = PacketValues.getVarInt(storageBase, commandOffEnd + 1L, bodyLimit);
        long labelAddress2 = commandOffEnd + 1L + PacketValues.varIntSize(labelBytes2);
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
        int commandOnBytes2 = PacketValues.getVarInt(storageBase, channelEnd2, bodyLimit);
        long commandOnAddress2 = channelEnd2 + PacketValues.varIntSize(commandOnBytes2);
        if ((commandOnBytes2 & 0xFFFFFFFFL) > 128
                || commandOnBytes2 > bodyLimit - commandOnAddress2)
            throw new DecoderException("Invalid text length");
        String commandOn$1 =
                PacketValues.getUtf8(storageBase, commandOnAddress2, commandOnBytes2, 32);
        long commandOnEnd2 = commandOnAddress2 + commandOnBytes2;
        int commandOffBytes2 = PacketValues.getVarInt(storageBase, commandOnEnd2, bodyLimit);
        long commandOffAddress2 = commandOnEnd2 + PacketValues.varIntSize(commandOffBytes2);
        if ((commandOffBytes2 & 0xFFFFFFFFL) > 128
                || commandOffBytes2 > bodyLimit - commandOffAddress2)
            throw new DecoderException("Invalid text length");
        String commandOff$1 =
                PacketValues.getUtf8(storageBase, commandOffAddress2, commandOffBytes2, 32);
        long commandOffEnd2 = commandOffAddress2 + commandOffBytes2;
        if (bodyLimit != commandOffEnd2) throw new DecoderException("Unexpected packet tail");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RBMKLeverBlockEntity receiver)) return;
        {
            int rowIndex = 0;
            boolean active = active$0;
            boolean polling = polling$0;
            String label = label$0;
            String channel = channel$0;
            String commandOn = commandOn$0;
            String commandOff = commandOff$0;
            receiver.configureLever(
                    rowIndex, active, polling, label, channel, commandOn, commandOff);
        }
        {
            int rowIndex = 1;
            boolean active = active$1;
            boolean polling = polling$1;
            String label = label$1;
            String channel = channel$1;
            String commandOn = commandOn$1;
            String commandOff = commandOff$1;
            receiver.configureLever(
                    rowIndex, active, polling, label, channel, commandOn, commandOff);
        }
        receiver.setChanged();
    }
}
