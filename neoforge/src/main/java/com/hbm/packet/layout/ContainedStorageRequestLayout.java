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

import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.action.ServerActions;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class ContainedStorageRequestLayout {
    public static final int VARIANT = 3;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 25;
    public static final int MAX_BODY_BYTES = 32767;

    private ContainedStorageRequestLayout() {}

    public static EncodedPayload encode(
            int rootContainer, int rootSlot, UUID target, boolean backpack) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyServerActions.TYPE, 25, MAX_BODY_BYTES, false, false, false);
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
            ByteBuf codecBuffer = encodedBuffer;
            {
                long rootContainerBits = rootContainer;
                long rootSlotBits = rootSlot;
                long backpackBits = backpack ? 1L : 0L;
                U.putLong(
                        storageBase,
                        storageAddress,
                        3L | ((rootContainerBits & 0xFFFFFFFFL) << 5) | (rootSlotBits << 37));
                U.putByte(
                        storageBase,
                        storageAddress + 8L,
                        (byte) (((rootSlotBits >>> 27) & 31L) | (backpackBits << 5)));
            }
            encodedBuffer.writerIndex(bufferStart + 9);
            UUIDUtil.STREAM_CODEC.encode(codecBuffer, target);
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
        ByteBuf codecBuffer = decodedBuffer;
        long rootContainerToRootSlotWord = U.getLong(storageBase, storageAddress);
        long rootSlotToBackpackWord = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        int rootContainer = (int) (rootContainerToRootSlotWord >>> 5);
        int rootSlot =
                (int) ((rootContainerToRootSlotWord >>> 37) | (rootSlotToBackpackWord << 27));
        boolean backpack = ((rootSlotToBackpackWord >>> 5) & 1L) != 0;
        if ((rootSlotToBackpackWord >>> 6) != 0)
            throw new DecoderException("Nonzero packet padding");
        decodedBuffer.readerIndex(bufferStart + 9);
        UUID target = UUIDUtil.STREAM_CODEC.decode(codecBuffer);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        if (!(context.player() instanceof ServerPlayer player)) return;
        ServerActions.containedStorage(player, rootContainer, rootSlot, target, backpack);
    }
}
