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

import com.hbm.blockentity.machine.reactor.ReactorControlBlockEntity;
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

public final class ReactorControlCurveLayout {
    public static final int VARIANT = 1;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 34;
    public static final int MAX_BODY_BYTES = 34;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private ReactorControlCurveLayout() {}

    public static EncodedPayload encode(
            int containerId,
            double levelLower,
            double levelUpper,
            double heatLower,
            double heatUpper) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        if (!(levelLower >= 0.0 && levelLower <= 100.0))
            throw new IllegalArgumentException("Invalid levelLower");
        if (!(levelUpper >= 0.0 && levelUpper <= 100.0))
            throw new IllegalArgumentException("Invalid levelUpper");
        if (!(heatLower >= 0.0 && heatLower <= 50000.0))
            throw new IllegalArgumentException("Invalid heatLower");
        if (!(heatUpper >= 0.0 && heatUpper <= 50000.0))
            throw new IllegalArgumentException("Invalid heatUpper");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyControls.TYPE, BODY_BYTES, MAX_BODY_BYTES, false, false, false);
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
            long levelLowerBits = doubleToRawLongBits(levelLower);
            long levelUpperBits = doubleToRawLongBits(levelUpper);
            long heatLowerBits = doubleToRawLongBits(heatLower);
            long heatUpperBits = doubleToRawLongBits(heatUpper);
            U.putLong(
                    storageBase,
                    storageAddress,
                    1L | (containerIdBits << 8) | (levelLowerBits << 15));
            U.putLong(
                    storageBase,
                    storageAddress + 8L,
                    (levelLowerBits >>> 49) | (levelUpperBits << 15));
            U.putLong(
                    storageBase,
                    storageAddress + 16L,
                    (levelUpperBits >>> 49) | (heatLowerBits << 15));
            U.putLong(
                    storageBase,
                    storageAddress + 24L,
                    (heatLowerBits >>> 49) | (heatUpperBits << 15));
            U.putShort(storageBase, storageAddress + 32L, (short) ((heatUpperBits >>> 49)));
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
        long containerIdToLevelLowerWord = U.getLong(storageBase, storageAddress);
        long levelLowerToLevelUpperWord = U.getLong(storageBase, storageAddress + 8L);
        long levelUpperToHeatLowerWord = U.getLong(storageBase, storageAddress + 16L);
        long heatLowerToHeatUpperWord = U.getLong(storageBase, storageAddress + 24L);
        long heatUpperWord = (U.getShort(storageBase, storageAddress + 32L) & 65535L);
        long containerIdBits = ((containerIdToLevelLowerWord >>> 8) & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        double levelLower =
                longBitsToDouble(
                        ((containerIdToLevelLowerWord >>> 15)
                                | (levelLowerToLevelUpperWord << 49)));
        if (!(levelLower >= 0.0 && levelLower <= 100.0))
            throw new DecoderException("Invalid levelLower");
        double levelUpper =
                longBitsToDouble(
                        ((levelLowerToLevelUpperWord >>> 15) | (levelUpperToHeatLowerWord << 49)));
        if (!(levelUpper >= 0.0 && levelUpper <= 100.0))
            throw new DecoderException("Invalid levelUpper");
        double heatLower =
                longBitsToDouble(
                        ((levelUpperToHeatLowerWord >>> 15) | (heatLowerToHeatUpperWord << 49)));
        if (!(heatLower >= 0.0 && heatLower <= 50000.0))
            throw new DecoderException("Invalid heatLower");
        double heatUpper =
                longBitsToDouble(((heatLowerToHeatUpperWord >>> 15) | (heatUpperWord << 49)));
        if (!(heatUpper >= 0.0 && heatUpper <= 50000.0))
            throw new DecoderException("Invalid heatUpper");
        if ((heatUpperWord >>> 15) != 0) throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof ReactorControlBlockEntity receiver)) return;
        receiver.setBand(levelLower, levelUpper, heatLower, heatUpper);
    }
}
