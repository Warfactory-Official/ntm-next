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

import com.hbm.extprop.PlayerState;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

public final class PlayerPropsLayout {
    public static final int VARIANT = 31;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 13;
    public static final int MAX_BODY_BYTES = 13;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private PlayerPropsLayout() {}

    public static EncodedPayload encode(
            float shield,
            float maxShield,
            int reputation,
            boolean enableBackpack,
            boolean enableHUD,
            boolean enableMagnet) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyClientEvents.TYPE,
                        BODY_BYTES,
                        MAX_BODY_BYTES,
                        true,
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
            long shieldBits = floatToRawIntBits(shield);
            long maxShieldBits = floatToRawIntBits(maxShield);
            long reputationBits = reputation;
            long enableBackpackBits = enableBackpack ? 1L : 0L;
            long enableHUDBits = enableHUD ? 1L : 0L;
            long enableMagnetBits = enableMagnet ? 1L : 0L;
            U.putLong(
                    storageBase,
                    storageAddress,
                    31L | ((shieldBits & 0xFFFFFFFFL) << 5) | (maxShieldBits << 37));
            U.putLong(
                    storageBase,
                    storageAddress + 5L,
                    ((maxShieldBits >>> 3) & 0x1FFFFFFFL)
                            | ((reputationBits & 0xFFFFFFFFL) << 29)
                            | (enableBackpackBits << 61)
                            | (enableHUDBits << 62)
                            | (enableMagnetBits << 63));
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
        long shieldToMaxShieldWord = U.getLong(storageBase, storageAddress);
        long maxShieldToEnableMagnetWord = U.getLong(storageBase, storageAddress + 5L);
        float shield = intBitsToFloat((int) (shieldToMaxShieldWord >>> 5));
        float maxShield =
                intBitsToFloat(
                        (int)
                                ((shieldToMaxShieldWord >>> 37)
                                        | ((maxShieldToEnableMagnetWord >>> 24) << 27)));
        int reputation = (int) (maxShieldToEnableMagnetWord >>> 29);
        boolean enableBackpack = ((maxShieldToEnableMagnetWord >>> 61) & 1L) != 0;
        boolean enableHUD = ((maxShieldToEnableMagnetWord >>> 62) & 1L) != 0;
        boolean enableMagnet = (maxShieldToEnableMagnetWord >>> 63) != 0;
        var player = context.playerOrNull();
        if (player == null) return;
        PlayerState.acceptMirror(
                player, shield, maxShield, reputation, enableBackpack, enableHUD, enableMagnet);
    }
}
