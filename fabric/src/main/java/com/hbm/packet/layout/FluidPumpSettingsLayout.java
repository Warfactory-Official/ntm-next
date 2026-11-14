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

import com.hbm.blockentity.network.FluidPumpBlockEntity;
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

public final class FluidPumpSettingsLayout {
    public static final int VARIANT = 93;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 3;
    public static final int MAX_BODY_BYTES = 11;
    private static final ConnectionPriority[] priorityValues = ConnectionPriority.VALUES;

    static {
        if (priorityValues.length != 5)
            throw new ExceptionInInitializerError("Enum table size changed: priority");
    }

    private FluidPumpSettingsLayout() {}

    public static EncodedPayload encode(
            int containerId,
            boolean ratePresent,
            int rate,
            boolean pressurePresent,
            int pressure,
            boolean priorityPresent,
            ConnectionPriority priority) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        int bodyBytes =
                ((25
                                + (ratePresent ? 32 : 0)
                                + (pressurePresent ? 32 : 0)
                                + (priorityPresent ? 3 : 0))
                        >>> 3);
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
            U.putByte(storageBase, storageAddress, (byte) (93L));
            long settingsWord = containerIdBits | ((ratePresent ? 1L : 0L) << 7);
            long settingsWordPart2 = 0L;
            if (ratePresent) {
                long rateBits = rate & 0xFFFFFFFFL;
                settingsWord |= (rateBits << (8));
            }
            int settingsBitOffset = ratePresent ? 40 : 8;
            settingsWord |= ((pressurePresent ? 1L : 0L) << (settingsBitOffset));
            if (pressurePresent) {
                long pressureBits = pressure & 0xFFFFFFFFL;
                settingsWord |= (pressureBits << (settingsBitOffset + 1));
                if ((settingsBitOffset + 1) > 32) settingsWordPart2 |= (pressureBits >>> (23));
            }
            settingsBitOffset += pressurePresent ? 33 : 1;
            if ((settingsBitOffset) < 64)
                settingsWord |= ((priorityPresent ? 1L : 0L) << (settingsBitOffset));
            if ((settingsBitOffset) >= 64)
                settingsWordPart2 |= ((priorityPresent ? 1L : 0L) << (9));
            if (priorityPresent) {
                long priorityBits = priority.ordinal();
                if ((settingsBitOffset + 1) < 64)
                    settingsWord |= (priorityBits << (settingsBitOffset + 1));
                if ((settingsBitOffset + 1) >= 64) settingsWordPart2 |= (priorityBits << (10));
            }
            switch (bodyBytes) {
                case 3 -> {
                    U.putShort(storageBase, storageAddress + 1L, (short) (settingsWord));
                }
                case 7 -> {
                    U.putInt(storageBase, storageAddress + 1L, (int) (settingsWord));
                    U.putShort(storageBase, storageAddress + 5L, (short) (settingsWord >>> 32));
                }
                case 11 -> {
                    U.putLong(storageBase, storageAddress + 1L, settingsWord);
                    U.putShort(storageBase, storageAddress + 9L, (short) (settingsWordPart2));
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
        long settingsWord;
        long settingsWordPart2;
        switch (bodyBytes) {
            case 3 -> {
                settingsWord = (U.getShort(storageBase, storageAddress + 1L) & 65535L);
                settingsWordPart2 = 0L;
            }
            case 7 -> {
                settingsWord =
                        ((U.getInt(storageBase, storageAddress + 1L) & 0xFFFFFFFFL)
                                | ((U.getShort(storageBase, storageAddress + 5L) & 65535L) << 32));
                settingsWordPart2 = 0L;
            }
            case 11 -> {
                settingsWord = U.getLong(storageBase, storageAddress + 1L);
                settingsWordPart2 = (U.getShort(storageBase, storageAddress + 9L) & 65535L);
            }
            default -> throw new DecoderException("Invalid packet length");
        }
        long containerIdBits = (settingsWord & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        boolean ratePresent = ((settingsWord >>> (7)) & 1L) != 0;
        int rate = 0;
        if (ratePresent) {
            long rateBits = ((settingsWord >>> (8)) & 0xFFFFFFFFL);
            rate = (int) rateBits;
        }
        int settingsBitOffset = ratePresent ? 40 : 8;
        boolean pressurePresent = ((settingsWord >>> (settingsBitOffset)) & 1L) != 0;
        int pressure = 0;
        if (pressurePresent) {
            long pressureBits =
                    (((settingsWord >>> (settingsBitOffset + 1))
                                    | ((settingsBitOffset + 1) > 32
                                            ? (settingsWordPart2 << (64 - (settingsBitOffset + 1)))
                                            : 0L))
                            & 0xFFFFFFFFL);
            pressure = (int) pressureBits;
        }
        settingsBitOffset += pressurePresent ? 33 : 1;
        boolean priorityPresent =
                (((settingsBitOffset) < 64
                                        ? (settingsWord >>> (settingsBitOffset))
                                        : (settingsWordPart2 >>> (9)))
                                & 1L)
                        != 0;
        ConnectionPriority priority = null;
        if (priorityPresent) {
            long priorityBits =
                    (((settingsBitOffset + 1) < 64
                                    ? (settingsWord >>> (settingsBitOffset + 1))
                                    : (settingsWordPart2 >>> (10)))
                            & 7L);
            if (priorityBits >= 5L) throw new DecoderException("Invalid priority ordinal");
            priority = priorityValues[(int) priorityBits];
        }
        settingsBitOffset += priorityPresent ? 4 : 1;
        if (bodyBytes != 1 + (((settingsBitOffset) + 7) >>> 3))
            throw new DecoderException("Unexpected packet tail");
        if ((((settingsBitOffset) <= 64 ? settingsWord : settingsWordPart2) >>> (settingsBitOffset))
                != 0) throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof FluidPumpBlockEntity receiver)) return;
        receiver.setControlSettings(
                ratePresent, rate, pressurePresent, pressure, priorityPresent, priority);
    }
}
