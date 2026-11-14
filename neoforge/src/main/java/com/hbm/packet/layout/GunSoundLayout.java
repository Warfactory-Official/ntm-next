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
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketBuffers;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

public final class GunSoundLayout {
    public static final int VARIANT = 1;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 35;
    public static final int MAX_BODY_BYTES = 1048576;
    private static final SoundSource[] sourceValues = SoundSource.values();

    static {
        if (sourceValues.length != 11)
            throw new ExceptionInInitializerError("Enum table size changed: source");
    }

    private GunSoundLayout() {}

    public static EncodedPayload encode(
            boolean legacy,
            int emitterId,
            Holder<SoundEvent> sound,
            SoundSource source,
            int x,
            int y,
            int z,
            float volume,
            float pitch,
            long seed) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyClientEvents.TYPE, 35, MAX_BODY_BYTES, true, false, true);
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
            var codecBuffer = PacketBuffers.registry(encodedBuffer, true);
            {
                long legacyBits = legacy ? 1L : 0L;
                long emitterIdBits = emitterId;
                long sourceBits = source.ordinal();
                long xBits = x;
                long yBits = y;
                long zBits = z;
                long volumeBits = floatToRawIntBits(volume);
                long pitchBits = floatToRawIntBits(pitch);
                long seedBits = seed;
                U.putLong(
                        storageBase,
                        storageAddress,
                        1L
                                | (legacyBits << 5)
                                | ((emitterIdBits & 0xFFFFFFFFL) << 6)
                                | (sourceBits << 38)
                                | (xBits << 42));
                U.putLong(
                        storageBase,
                        storageAddress + 8L,
                        ((xBits >>> 22) & 0x3FFL) | ((yBits & 0xFFFFFFFFL) << 10) | (zBits << 42));
                U.putLong(
                        storageBase,
                        storageAddress + 16L,
                        ((zBits >>> 22) & 0x3FFL)
                                | ((volumeBits & 0xFFFFFFFFL) << 10)
                                | (pitchBits << 42));
                U.putLong(
                        storageBase,
                        storageAddress + 24L,
                        ((pitchBits >>> 22) & 0x3FFL) | (seedBits << 10));
                U.putShort(storageBase, storageAddress + 32L, (short) ((seedBits >>> 54)));
            }
            encodedBuffer.writerIndex(bufferStart + 34);
            SoundEvent.STREAM_CODEC.encode(codecBuffer, sound);
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
        var codecBuffer = PacketBuffers.registry(decodedBuffer, context);
        long legacyToXWord = U.getLong(storageBase, storageAddress);
        long xToZWord = U.getLong(storageBase, storageAddress + 8L);
        long zToPitchWord = U.getLong(storageBase, storageAddress + 16L);
        long pitchToSeedWord = U.getLong(storageBase, storageAddress + 24L);
        long seedWord = (U.getShort(storageBase, storageAddress + 32L) & 65535L);
        boolean legacy = ((legacyToXWord >>> 5) & 1L) != 0;
        int emitterId = (int) (legacyToXWord >>> 6);
        long sourceBits = ((legacyToXWord >>> 38) & 15L);
        if (sourceBits >= 11L) throw new DecoderException("Invalid source ordinal");
        SoundSource source = sourceValues[(int) sourceBits];
        int x = (int) ((legacyToXWord >>> 42) | (xToZWord << 22));
        int y = (int) (xToZWord >>> 10);
        int z = (int) ((xToZWord >>> 42) | (zToPitchWord << 22));
        float volume = intBitsToFloat((int) (zToPitchWord >>> 10));
        float pitch = intBitsToFloat((int) ((zToPitchWord >>> 42) | (pitchToSeedWord << 22)));
        long seed = ((pitchToSeedWord >>> 10) | (seedWord << 54));
        if ((seedWord >>> 10) != 0) throw new DecoderException("Nonzero packet padding");
        decodedBuffer.readerIndex(bufferStart + 34);
        Holder<SoundEvent> sound = SoundEvent.STREAM_CODEC.decode(codecBuffer);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        var player = context.playerOrNull();
        if (player == null) return;
        ClientPacketEvents.gunSound(
                player, legacy, emitterId, sound, source, x, y, z, volume, pitch, seed);
    }
}
