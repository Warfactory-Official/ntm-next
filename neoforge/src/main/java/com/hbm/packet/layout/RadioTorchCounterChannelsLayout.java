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

import com.hbm.blockentity.network.RadioCounterBlockEntity;
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

public final class RadioTorchCounterChannelsLayout {
    public static final int VARIANT = 129;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 5;
    public static final int MAX_BODY_BYTES = 130;

    public interface Channels {
        String channel(int index);
    }

    private RadioTorchCounterChannelsLayout() {}

    public static EncodedPayload encode(int containerId, Channels channels) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        long bodyBits = 16L;
        for (int elementIndex = 0; elementIndex < 3; elementIndex++) {
            {
                int textBytes = PacketValues.utf8Length(channels.channel(elementIndex), 40, 10);
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
                U.putShort(storageBase, storageAddress, (short) (129L | (containerIdBits << 8)));
            }
            long elementAddress = storageAddress + 2L;
            for (int elementIndex = 0; elementIndex < 3; elementIndex++) {
                elementAddress =
                        PacketValues.putUtf8(
                                storageBase, elementAddress, channels.channel(elementIndex));
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
        if ((channelBytes & 0xFFFFFFFFL) > 40 || channelBytes > bodyLimit - channelAddress)
            throw new DecoderException("Invalid text length");
        String channel$0 = PacketValues.getUtf8(storageBase, channelAddress, channelBytes, 10);
        long channelEnd = channelAddress + channelBytes;
        int channelBytes2 = PacketValues.getVarInt(storageBase, channelEnd, bodyLimit);
        long channelAddress2 = channelEnd + PacketValues.varIntSize(channelBytes2);
        if ((channelBytes2 & 0xFFFFFFFFL) > 40 || channelBytes2 > bodyLimit - channelAddress2)
            throw new DecoderException("Invalid text length");
        String channel$1 = PacketValues.getUtf8(storageBase, channelAddress2, channelBytes2, 10);
        long channelEnd2 = channelAddress2 + channelBytes2;
        int channelBytes3 = PacketValues.getVarInt(storageBase, channelEnd2, bodyLimit);
        long channelAddress3 = channelEnd2 + PacketValues.varIntSize(channelBytes3);
        if ((channelBytes3 & 0xFFFFFFFFL) > 40 || channelBytes3 > bodyLimit - channelAddress3)
            throw new DecoderException("Invalid text length");
        String channel$2 = PacketValues.getUtf8(storageBase, channelAddress3, channelBytes3, 10);
        long channelEnd3 = channelAddress3 + channelBytes3;
        if (bodyLimit != channelEnd3) throw new DecoderException("Unexpected packet tail");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RadioCounterBlockEntity receiver)) return;
        {
            int elementIndex = 0;
            String channel = channel$0;
            receiver.setChannel(elementIndex, channel);
        }
        {
            int elementIndex = 1;
            String channel = channel$1;
            receiver.setChannel(elementIndex, channel);
        }
        {
            int elementIndex = 2;
            String channel = channel$2;
            receiver.setChannel(elementIndex, channel);
        }
    }
}
