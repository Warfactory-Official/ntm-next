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

import com.hbm.config.ModeStatus;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class ModeSettingsLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 3;
    public static final int MIN_BODY_BYTES = 1;
    public static final int MAX_BODY_BYTES = 1;
    public static final int BODY_BYTES = MIN_BODY_BYTES;

    private ModeSettingsLayout() {}

    public static EncodedPayload encode(int badges, boolean rtgDecay, boolean rtgScalePower) {
        if (((long) badges & 0xFFFFFFFFL) > 7L)
            throw new IllegalArgumentException("Invalid badges");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilySyncValues.TYPE,
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
            long badgesBits = (long) badges;
            long rtgDecayBits = rtgDecay ? 1L : 0L;
            long rtgScalePowerBits = rtgScalePower ? 1L : 0L;
            U.putByte(
                    storageBase,
                    storageAddress,
                    (byte) ((badgesBits << 3) | (rtgDecayBits << 6) | (rtgScalePowerBits << 7)));
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
        long badgesToRtgScalePowerWord = (U.getByte(storageBase, storageAddress) & 255L);
        int badges = (int) ((badgesToRtgScalePowerWord >>> 3) & 7L);
        boolean rtgDecay = ((badgesToRtgScalePowerWord >>> 6) & 1L) != 0;
        boolean rtgScalePower = (badgesToRtgScalePowerWord >>> 7) != 0;
        ModeStatus.accept(badges, rtgDecay, rtgScalePower);
    }
}
