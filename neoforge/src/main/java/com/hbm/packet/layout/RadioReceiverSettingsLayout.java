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

import com.hbm.blockentity.machine.radio.RadioReceiverBlockEntity;
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

public final class RadioReceiverSettingsLayout {
    public static final int VARIANT = 69;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 3;
    public static final int MAX_BODY_BYTES = 45;

    private RadioReceiverSettingsLayout() {}

    public static EncodedPayload encode(
            int containerId,
            boolean channelPresent,
            String channel,
            boolean statePresent,
            boolean state) {
        int channelBytes = channelPresent ? PacketValues.utf8Length(channel, 40, 10) : 0;
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        long bodyBits = 16L;
        if (channelPresent) {
            bodyBits += 8L * (PacketValues.varIntSize(channelBytes) + channelBytes);
        }
        bodyBits += 1L;
        if (statePresent) {
            bodyBits += 1L;
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
            {
                long containerIdBits = ((long) containerId - 1L);
                long channelPresentBits = channelPresent ? 1L : 0L;
                U.putShort(
                        storageBase,
                        storageAddress,
                        (short) (69L | (containerIdBits << 8) | (channelPresentBits << 15)));
            }
            bitOffset = 16L;
            if (channelPresent) {
                long channelEnd =
                        PacketValues.putUtf8(
                                storageBase,
                                storageAddress + (bitOffset >>> 3),
                                channel,
                                channelBytes);
                bitOffset = (channelEnd - storageAddress) * 8;
            }
            if (statePresent) {
                {
                    long stateBits = state ? 1L : 0L;
                    U.putByte(
                            storageBase,
                            storageAddress + (bitOffset >>> 3),
                            (byte) (1L | (stateBits << 1)));
                }
            } else {
                {
                    U.putByte(storageBase, storageAddress + (bitOffset >>> 3), (byte) (0L));
                }
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
        long bitOffset;
        long availableBits = (long) bodyBytes * 8;
        long bodyLimit = storageAddress + bodyBytes;
        long containerIdToChannelPresentWord = (U.getByte(storageBase, storageAddress + 1L) & 255L);
        long containerIdBits = (containerIdToChannelPresentWord & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        boolean channelPresent = (containerIdToChannelPresentWord >>> 7) != 0;
        String channel = null;
        bitOffset = 16L;
        if (channelPresent) {
            int channelBytes =
                    PacketValues.getVarInt(
                            storageBase, storageAddress + (bitOffset >>> 3), bodyLimit);
            long channelAddress =
                    storageAddress + (bitOffset >>> 3) + PacketValues.varIntSize(channelBytes);
            if ((channelBytes & 0xFFFFFFFFL) > 40 || channelBytes > bodyLimit - channelAddress)
                throw new DecoderException("Invalid text length");
            channel = PacketValues.getUtf8(storageBase, channelAddress, channelBytes, 10);
            long channelEnd = channelAddress + channelBytes;
            bitOffset = (channelEnd - storageAddress) * 8;
        }
        if (bitOffset + 1L > availableBits) throw new DecoderException("Truncated packet fields");
        long statePresentWord = (U.getByte(storageBase, storageAddress + (bitOffset >>> 3)) & 255L);
        boolean statePresent = (statePresentWord & 1L) != 0;
        bitOffset += 1L;
        boolean state = false;
        if (statePresent) {
            if (bitOffset + 1L > availableBits)
                throw new DecoderException("Truncated packet fields");
            long stateWord = (U.getByte(storageBase, storageAddress + (bitOffset >>> 3)) & 255L);
            state = ((stateWord >>> 1) & 1L) != 0;
            bitOffset += 1L;
        }
        if (bodyBytes != (int) ((bitOffset + 7L) >>> 3))
            throw new DecoderException("Unexpected packet tail");
        if ((bitOffset & 7) != 0
                && ((U.getByte(storageBase, storageAddress + (bitOffset >>> 3)) & 255)
                                >>> (int) (bitOffset & 7))
                        != 0) throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RadioReceiverBlockEntity receiver)) return;
        receiver.updateSettings(channelPresent, channel, statePresent, state);
    }
}
