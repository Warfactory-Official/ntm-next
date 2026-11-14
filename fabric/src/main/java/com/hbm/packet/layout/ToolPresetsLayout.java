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

import com.hbm.handler.ability.ToolPreset;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.action.ServerActions;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class ToolPresetsLayout {
    public static final int VARIANT = 18;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 8;
    public static final int MAX_BODY_BYTES = 32767;

    private ToolPresetsLayout() {}

    public static EncodedPayload encode(int containerId, List<ToolPreset> presets, int current) {
        int presetsCount = presets.size();
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        if (presetsCount < 1 || presetsCount > 99)
            throw new IllegalArgumentException("Invalid presets count");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyServerActions.TYPE, 11, MAX_BODY_BYTES, false, false, false);
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
            ByteBuf codecBuffer = encodedBuffer;
            {
                long containerIdBits = ((long) containerId - 1L);
                U.putShort(storageBase, storageAddress, (short) (18L | (containerIdBits << 5)));
            }
            long presetsAddress =
                    PacketValues.putVarInt(storageBase, storageAddress + 2L, presetsCount);
            {
                long currentBits = current;
                U.putInt(storageBase, presetsAddress, (int) (currentBits));
            }
            encodedBuffer.writerIndex(bufferStart + (int) (presetsAddress - storageAddress) + 4);
            for (int elementIndex = 0; elementIndex < presetsCount; elementIndex++) {
                ToolPreset.STREAM_CODEC.encode(codecBuffer, presets.get(elementIndex));
            }
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
        if (bodyBytes < MIN_BODY_BYTES) throw new DecoderException("Invalid packet length");
        if (!(context.player() instanceof ServerPlayer player)) return;
        ByteBuf codecBuffer = decodedBuffer;
        long bodyLimit = storageAddress + bodyBytes;
        long containerIdWord = (U.getShort(storageBase, storageAddress) & 65535L);
        long containerIdBits = ((containerIdWord >>> 5) & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        if ((containerIdWord >>> 12) != 0) throw new DecoderException("Nonzero packet padding");
        int presetsCount = PacketValues.getVarInt(storageBase, storageAddress + 2L, bodyLimit);
        if ((presetsCount & 0xFFFFFFFFL) > 99L || presetsCount < 1)
            throw new DecoderException("Invalid collection count");
        long presetsAddress = storageAddress + 2L + PacketValues.varIntSize(presetsCount);
        if (bodyLimit - presetsAddress < 4) throw new DecoderException("Truncated packet fields");
        long currentWord = (U.getInt(storageBase, presetsAddress) & 0xFFFFFFFFL);
        int current = (int) currentWord;
        List<ToolPreset> presets = new ArrayList<>(presetsCount);
        decodedBuffer.readerIndex(bufferStart + (int) (presetsAddress - storageAddress) + 4);
        for (int elementIndex = 0; elementIndex < presetsCount; elementIndex++) {
            ToolPreset preset = ToolPreset.STREAM_CODEC.decode(codecBuffer);
            presets.add(preset);
        }
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        ServerActions.toolPresets(player, containerId, presets, current);
    }
}
