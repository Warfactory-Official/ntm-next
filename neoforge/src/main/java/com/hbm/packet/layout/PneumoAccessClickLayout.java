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

import com.hbm.inventory.container.PneumoStorageAccessMenu.ClickOp;
import com.hbm.inventory.container.PneumoStorageAccessMenu;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.action.ServerActions;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class PneumoAccessClickLayout {
    public static final int VARIANT = 12;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 6;
    public static final int MAX_BODY_BYTES = 6;
    public static final int BODY_BYTES = MIN_BODY_BYTES;
    private static final ClickOp[] opValues = PneumoStorageAccessMenu.ClickOp.values();

    static {
        if (opValues.length != 3)
            throw new ExceptionInInitializerError("Enum table size changed: op");
    }

    private PneumoAccessClickLayout() {}

    public static EncodedPayload encode(int containerId, ClickOp op, int entryId) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyServerActions.TYPE,
                        BODY_BYTES,
                        MAX_BODY_BYTES,
                        false,
                        false,
                        false);
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
            long containerIdBits = ((long) containerId - 1L);
            long opBits = op.ordinal();
            long entryIdBits = entryId;
            U.putInt(
                    storageBase,
                    storageAddress,
                    (int) (12L | (containerIdBits << 5) | (opBits << 12) | (entryIdBits << 14)));
            U.putShort(
                    storageBase, storageAddress + 4L, (short) (((entryIdBits >>> 18) & 0x3FFFL)));
            encodedBuffer.writerIndex(bufferStart + BODY_BYTES);
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
        if (bodyBytes != BODY_BYTES) throw new DecoderException("Invalid packet length");
        long containerIdToEntryIdWord = (U.getInt(storageBase, storageAddress) & 0xFFFFFFFFL);
        long entryIdWord = (U.getShort(storageBase, storageAddress + 4L) & 65535L);
        long containerIdBits = ((containerIdToEntryIdWord >>> 5) & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        long opBits = ((containerIdToEntryIdWord >>> 12) & 3L);
        if (opBits >= 3L) throw new DecoderException("Invalid op ordinal");
        ClickOp op = opValues[(int) opBits];
        int entryId = (int) ((containerIdToEntryIdWord >>> 14) | (entryIdWord << 18));
        if ((entryIdWord >>> 14) != 0) throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)) return;
        ServerActions.pneumoAccessClick(player, containerId, op, entryId);
    }
}
