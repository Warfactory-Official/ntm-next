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

import com.hbm.items.armor.ArmorFlight.Mode;
import com.hbm.items.armor.ArmorFlight;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.action.ServerActions;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class ArmorFlightLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 2;
    public static final int MAX_BODY_BYTES = 2;
    public static final int BODY_BYTES = MIN_BODY_BYTES;
    private static final Mode[] modeValues = ArmorFlight.Mode.VALUES;

    static {
        if (modeValues.length != 7)
            throw new ExceptionInInitializerError("Enum table size changed: mode");
    }

    private ArmorFlightLayout() {}

    public static EncodedPayload encode(Mode mode, boolean euphemiumFallCapped) {
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
            long modeBits = mode.ordinal();
            long euphemiumFallCappedBits = euphemiumFallCapped ? 1L : 0L;
            U.putShort(
                    storageBase,
                    storageAddress,
                    (short) ((modeBits << 5) | (euphemiumFallCappedBits << 8)));
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
        long modeToEuphemiumFallCappedWord = (U.getShort(storageBase, storageAddress) & 65535L);
        long modeBits = ((modeToEuphemiumFallCappedWord >>> 5) & 7L);
        if (modeBits >= 7L) throw new DecoderException("Invalid mode ordinal");
        Mode mode = modeValues[(int) modeBits];
        boolean euphemiumFallCapped = ((modeToEuphemiumFallCappedWord >>> 8) & 1L) != 0;
        if ((modeToEuphemiumFallCappedWord >>> 9) != 0)
            throw new DecoderException("Nonzero packet padding");
        if (!(context.player() instanceof ServerPlayer player)) return;
        ServerActions.armorFlight(player, mode, euphemiumFallCapped);
    }
}
