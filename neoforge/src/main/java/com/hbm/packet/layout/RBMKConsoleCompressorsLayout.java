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

public final class RBMKConsoleCompressorsLayout {
    public static final int VARIANT = 12;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 3;
    public static final int MAX_BODY_BYTES = 228;

    private RBMKConsoleCompressorsLayout() {}

    public static EncodedPayload encode(
            int containerId, boolean[] columns, ColumnSnapshot[] columnData) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        int columnsCount = 0;
        for (int elementIndex = 0; elementIndex < 225; elementIndex++) {
            if (columns[elementIndex]
                    && columnData[elementIndex] != null
                    && columnData[elementIndex].type() == RBMKColumnType.BOILER) columnsCount++;
        }
        int bodyBytes = 3 + columnsCount;
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
            U.putShort(storageBase, storageAddress, (short) (12L | (containerIdBits << 8)));
            U.putByte(storageBase, storageAddress + 2L, (byte) (columnsCount));
            long columnsAddress = storageAddress + 3L;
            for (int elementIndex = 0; elementIndex < 225; elementIndex++) {
                if (columns[elementIndex]
                        && columnData[elementIndex] != null
                        && columnData[elementIndex].type() == RBMKColumnType.BOILER) {
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
        long containerIdWord = (U.getShort(storageBase, storageAddress) & 65535L);
        long containerIdBits = ((containerIdWord >>> 8) & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        if ((containerIdWord >>> 15) != 0) throw new DecoderException("Nonzero packet padding");
        int columnsCount = U.getByte(storageBase, storageAddress + 2L) & 255;
        if (3 + columnsCount != bodyBytes) throw new DecoderException("Invalid columns length");
        long columnsAddress = storageAddress + 3L;
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
        receiver.cycleCompressors(storageBase, columnsAddress, columnsCount);
    }
}
