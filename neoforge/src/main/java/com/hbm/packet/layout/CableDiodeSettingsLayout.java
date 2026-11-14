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

import com.hbm.blockentity.network.CableDiodeBlockEntity;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.packet.ControlAuthority;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.uninos.ConnectionPriority;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class CableDiodeSettingsLayout {
    public static final int VARIANT = 85;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 3;
    public static final int MAX_BODY_BYTES = 11;
    private static final ConnectionPriority[] priorityValues = ConnectionPriority.VALUES;

    static {
        if (priorityValues.length != 5)
            throw new ExceptionInInitializerError("Enum table size changed: priority");
    }

    private CableDiodeSettingsLayout() {}

    public static EncodedPayload encode(
            int containerId,
            boolean limitPresent,
            long limit,
            boolean priorityPresent,
            ConnectionPriority priority) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        int bodyBytes = limitPresent ? 11 : 3;
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
            long containerIdBits = ((long) containerId - 1L);
            U.putByte(storageBase, storageAddress, (byte) (85L));
            long settingsWord = containerIdBits | ((limitPresent ? 1L : 0L) << 7);
            long settingsWordPart2 = 0L;
            if (limitPresent) {
                long limitBits = limit;
                settingsWord |= (limitBits << (8));
                settingsWordPart2 |= (limitBits >>> (56));
            }
            int settingsBitOffset = limitPresent ? 72 : 8;
            if ((settingsBitOffset) < 64) settingsWord |= ((priorityPresent ? 1L : 0L) << (8));
            if ((settingsBitOffset) >= 64)
                settingsWordPart2 |= ((priorityPresent ? 1L : 0L) << (8));
            if (priorityPresent) {
                long priorityBits = priority.ordinal();
                if ((settingsBitOffset + 1) < 64) settingsWord |= (priorityBits << (9));
                if ((settingsBitOffset + 1) >= 64) settingsWordPart2 |= (priorityBits << (9));
            }
            if (bodyBytes == 3) {
                U.putShort(storageBase, storageAddress + 1L, (short) (settingsWord));
            } else {
                U.putLong(storageBase, storageAddress + 1L, settingsWord);
                U.putShort(storageBase, storageAddress + 9L, (short) (settingsWordPart2));
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
        long settingsWord;
        long settingsWordPart2;
        if (bodyBytes == 3) {
            settingsWord = (U.getShort(storageBase, storageAddress + 1L) & 65535L);
            settingsWordPart2 = 0L;
        } else {
            if (bodyBytes != 11) throw new DecoderException("Invalid packet length");
            settingsWord = U.getLong(storageBase, storageAddress + 1L);
            settingsWordPart2 = (U.getShort(storageBase, storageAddress + 9L) & 65535L);
        }
        long containerIdBits = (settingsWord & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        boolean limitPresent = ((settingsWord >>> (7)) & 1L) != 0;
        long limit = 0;
        if (limitPresent) {
            long limitBits = ((settingsWord >>> (8)) | (settingsWordPart2 << (56)));
            limit = limitBits;
        }
        int settingsBitOffset = limitPresent ? 72 : 8;
        boolean priorityPresent =
                (((settingsBitOffset) < 64 ? (settingsWord >>> (8)) : (settingsWordPart2 >>> (8)))
                                & 1L)
                        != 0;
        ConnectionPriority priority = null;
        if (priorityPresent) {
            long priorityBits =
                    (((settingsBitOffset + 1) < 64
                                    ? (settingsWord >>> (9))
                                    : (settingsWordPart2 >>> (9)))
                            & 7L);
            if (priorityBits >= 5L) throw new DecoderException("Invalid priority ordinal");
            priority = priorityValues[(int) priorityBits];
        }
        settingsBitOffset += priorityPresent ? 4 : 1;
        if (bodyBytes != (limitPresent ? 11 : 3))
            throw new DecoderException("Unexpected packet tail");
        if ((((settingsBitOffset) <= 64 ? settingsWord : settingsWordPart2) >>> (settingsBitOffset))
                != 0) throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof CableDiodeBlockEntity receiver)) return;
        receiver.applySettings(limitPresent, limit, priorityPresent, priority);
    }
}
