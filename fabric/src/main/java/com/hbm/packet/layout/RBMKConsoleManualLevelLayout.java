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

import com.hbm.blockentity.machine.rbmk.ColumnSnapshot;
import com.hbm.blockentity.machine.rbmk.RBMKColumnType;
import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.packet.ControlAuthority;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

public final class RBMKConsoleManualLevelLayout {
    public static final int VARIANT = 8;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 11;
    public static final int MAX_BODY_BYTES = 236;

    private RBMKConsoleManualLevelLayout() {}

    public static EncodedPayload encode(
            int containerId,
            double level,
            boolean[] columns,
            boolean all,
            ColumnSnapshot[] columnData) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        if (!(level >= 0.0 && level <= 1.0)) throw new IllegalArgumentException("Invalid level");
        int columnsCount = 0;
        for (int elementIndex = 0; elementIndex < 225; elementIndex++) {
            if (all
                    ? columnData[elementIndex] != null
                            && columnData[elementIndex].type() == RBMKColumnType.CONTROL
                    : columns[elementIndex]) columnsCount++;
        }
        int bodyBytes = 11 + columnsCount;
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
            long levelBits = doubleToRawLongBits(level);
            U.putLong(storageBase, storageAddress, 8L | (containerIdBits << 8) | (levelBits << 15));
            U.putShort(storageBase, storageAddress + 8L, (short) ((levelBits >>> 49)));
            U.putByte(storageBase, storageAddress + 10L, (byte) (columnsCount));
            long columnsAddress = storageAddress + 11L;
            for (int elementIndex = 0; elementIndex < 225; elementIndex++) {
                if (all
                        ? columnData[elementIndex] != null
                                && columnData[elementIndex].type() == RBMKColumnType.CONTROL
                        : columns[elementIndex]) {
                    U.putByte(storageBase, columnsAddress, (byte) (elementIndex));
                    columnsAddress += 1L;
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
        if (bodyBytes < MIN_BODY_BYTES || bodyBytes > MAX_BODY_BYTES)
            throw new DecoderException("Invalid packet length");
        long containerIdToLevelWord = U.getLong(storageBase, storageAddress);
        long levelWord = (U.getShort(storageBase, storageAddress + 8L) & 65535L);
        long containerIdBits = ((containerIdToLevelWord >>> 8) & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        double level = longBitsToDouble(((containerIdToLevelWord >>> 15) | (levelWord << 49)));
        if (!(level >= 0.0 && level <= 1.0)) throw new DecoderException("Invalid level");
        if ((levelWord >>> 15) != 0) throw new DecoderException("Nonzero packet padding");
        int columnsCount = U.getByte(storageBase, storageAddress + 10L) & 255;
        if (11 + columnsCount != bodyBytes) throw new DecoderException("Invalid columns length");
        long columnsAddress = storageAddress + 11L;
        for (long columnAddress = columnsAddress;
                columnAddress < storageAddress + bodyBytes;
                columnAddress += 1L) {
            if ((U.getByte(storageBase, columnAddress) & 255L) > 224L)
                throw new DecoderException("Invalid column");
        }
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof RBMKConsoleBlockEntity receiver)) return;
        receiver.orderRods(storageBase, columnsAddress, columnsCount, level);
    }
}
