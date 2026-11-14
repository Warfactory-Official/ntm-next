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

import com.hbm.client.ClientPacketEvents;
import com.hbm.items.weapon.grenade.GrenadeShellItem.EnumGrenadeShell;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class GrenadeAnimationLayout {
    public static final int VARIANT = 2;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 1;
    public static final int MAX_BODY_BYTES = 1;
    public static final int BODY_BYTES = MIN_BODY_BYTES;
    private static final EnumGrenadeShell[] shellValues = EnumGrenadeShell.VALUES;

    static {
        if (shellValues.length != 4)
            throw new ExceptionInInitializerError("Enum table size changed: shell");
    }

    private GrenadeAnimationLayout() {}

    public static EncodedPayload encode(EnumGrenadeShell shell) {
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
            long shellBits = shell.ordinal();
            U.putByte(storageBase, storageAddress, (byte) (2L | (shellBits << 5)));
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
        long shellWord = (U.getByte(storageBase, storageAddress) & 255L);
        EnumGrenadeShell shell = shellValues[(int) ((shellWord >>> 5) & 3L)];
        if ((shellWord >>> 7) != 0) throw new DecoderException("Nonzero packet padding");
        ClientPacketEvents.grenadeAnimation(shell);
    }
}
