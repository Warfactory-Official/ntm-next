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

import com.hbm.blockentity.machine.ExcavatorBlockEntity;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.packet.ControlAuthority;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class ExcavatorSettingsLayout {
    public static final int VARIANT = 55;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 3;
    public static final int MAX_BODY_BYTES = 3;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private ExcavatorSettingsLayout() {}

    public static EncodedPayload encode(
            int containerId,
            boolean drill,
            boolean crusher,
            boolean walling,
            boolean veinminer,
            boolean silktouch) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
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
            long drillBits = drill ? 1L : 0L;
            long crusherBits = crusher ? 1L : 0L;
            long wallingBits = walling ? 1L : 0L;
            long veinminerBits = veinminer ? 1L : 0L;
            long silktouchBits = silktouch ? 1L : 0L;
            U.putShort(
                    storageBase,
                    storageAddress,
                    (short) (55L | (containerIdBits << 8) | (drillBits << 15)));
            U.putByte(
                    storageBase,
                    storageAddress + 2L,
                    (byte)
                            (crusherBits
                                    | (wallingBits << 1)
                                    | (veinminerBits << 2)
                                    | (silktouchBits << 3)));
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
        long containerIdToSilktouchWord = (U.getShort(storageBase, storageAddress + 1L) & 65535L);
        long containerIdBits = (containerIdToSilktouchWord & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        boolean drill = ((containerIdToSilktouchWord >>> 7) & 1L) != 0;
        boolean crusher = ((containerIdToSilktouchWord >>> 8) & 1L) != 0;
        boolean walling = ((containerIdToSilktouchWord >>> 9) & 1L) != 0;
        boolean veinminer = ((containerIdToSilktouchWord >>> 10) & 1L) != 0;
        boolean silktouch = ((containerIdToSilktouchWord >>> 11) & 1L) != 0;
        if ((containerIdToSilktouchWord >>> 12) != 0)
            throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof ExcavatorBlockEntity receiver)) return;
        receiver.setControlToggles(drill, crusher, walling, veinminer, silktouch);
    }
}
