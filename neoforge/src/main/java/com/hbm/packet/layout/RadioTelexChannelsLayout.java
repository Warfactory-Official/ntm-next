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

public final class RadioTelexChannelsLayout {
    public static final int VARIANT = 102;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 4;
    public static final int MAX_BODY_BYTES = 86;

    private RadioTelexChannelsLayout() {}

    public static EncodedPayload encode(int containerId, String txChannel, String rxChannel) {
        int txChannelBytes = PacketValues.utf8Length(txChannel, 40, 10);
        int rxChannelBytes = PacketValues.utf8Length(rxChannel, 40, 10);
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        int bodyBytes =
                2
                        + (PacketValues.varIntSize(txChannelBytes) + txChannelBytes)
                        + (PacketValues.varIntSize(rxChannelBytes) + rxChannelBytes);
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
                U.putShort(storageBase, storageAddress, (short) (102L | (containerIdBits << 8)));
            }
            long txChannelEnd =
                    PacketValues.putUtf8(
                            storageBase, storageAddress + 2L, txChannel, txChannelBytes);
            PacketValues.putUtf8(storageBase, txChannelEnd, rxChannel, rxChannelBytes);
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
        int txChannelBytes = PacketValues.getVarInt(storageBase, storageAddress + 2L, bodyLimit);
        long txChannelAddress = storageAddress + 2L + PacketValues.varIntSize(txChannelBytes);
        if ((txChannelBytes & 0xFFFFFFFFL) > 40 || txChannelBytes > bodyLimit - txChannelAddress)
            throw new DecoderException("Invalid text length");
        String txChannel = PacketValues.getUtf8(storageBase, txChannelAddress, txChannelBytes, 10);
        long txChannelEnd = txChannelAddress + txChannelBytes;
        int rxChannelBytes = PacketValues.getVarInt(storageBase, txChannelEnd, bodyLimit);
        long rxChannelAddress = txChannelEnd + PacketValues.varIntSize(rxChannelBytes);
        if ((rxChannelBytes & 0xFFFFFFFFL) > 40 || rxChannelBytes > bodyLimit - rxChannelAddress)
            throw new DecoderException("Invalid text length");
        String rxChannel = PacketValues.getUtf8(storageBase, rxChannelAddress, rxChannelBytes, 10);
        long rxChannelEnd = rxChannelAddress + rxChannelBytes;
        if (bodyLimit != rxChannelEnd) throw new DecoderException("Unexpected packet tail");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RadioTelexBlockEntity receiver)) return;
        receiver.setChannels(txChannel, rxChannel);
    }
}
