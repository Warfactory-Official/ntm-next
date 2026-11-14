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
import com.hbm.extprop.ContaminationEffect;
import com.hbm.extprop.LivingHazards;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.List;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

public final class HbmPlayerSyncLayout {
    public static final int VARIANT = 21;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 46;
    public static final int MAX_BODY_BYTES = 1048576;

    private HbmPlayerSyncLayout() {}

    public static EncodedPayload encode(
            double radiation,
            double digamma,
            double radBuf,
            int asbestos,
            int bombTimer,
            int contagion,
            int blacklung,
            int oil,
            List<ContaminationEffect> contamination) {
        int contaminationCount = contamination.size();
        if (contaminationCount > 2097151)
            throw new IllegalArgumentException("Invalid contamination count");
        long bodyBits = 360L + 8L * (PacketValues.varIntSize(contaminationCount));
        bodyBits += (long) contaminationCount * 129L;
        if (bodyBits > 8388608L) throw new EncoderException("Packet exceeds frame limit");
        int bodyBytes = (int) ((bodyBits + 7L) >>> 3);
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyClientEvents.TYPE,
                        bodyBytes,
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
            long bitOffset;
            long partialByte = 0L;
            {
                long radiationBits = doubleToRawLongBits(radiation);
                long digammaBits = doubleToRawLongBits(digamma);
                long radBufBits = doubleToRawLongBits(radBuf);
                long asbestosBits = asbestos;
                long bombTimerBits = bombTimer;
                long contagionBits = contagion;
                long blacklungBits = blacklung;
                long oilBits = oil;
                U.putLong(storageBase, storageAddress, 21L | (radiationBits << 5));
                U.putLong(
                        storageBase,
                        storageAddress + 8L,
                        (radiationBits >>> 59) | (digammaBits << 5));
                U.putLong(
                        storageBase,
                        storageAddress + 16L,
                        (digammaBits >>> 59) | (radBufBits << 5));
                U.putLong(
                        storageBase,
                        storageAddress + 24L,
                        (radBufBits >>> 59)
                                | ((asbestosBits & 0xFFFFFFFFL) << 5)
                                | (bombTimerBits << 37));
                U.putLong(
                        storageBase,
                        storageAddress + 32L,
                        ((bombTimerBits >>> 27) & 31L)
                                | ((contagionBits & 0xFFFFFFFFL) << 5)
                                | (blacklungBits << 37));
                U.putLong(
                        storageBase,
                        storageAddress + 37L,
                        ((blacklungBits >>> 3) & 0x1FFFFFFFL) | ((oilBits & 0xFFFFFFFFL) << 29));
            }
            long contaminationAddress =
                    PacketValues.putVarInt(storageBase, storageAddress + 45L, contaminationCount);
            bitOffset = (contaminationAddress - storageAddress) * 8;
            for (int rowIndex = 0; rowIndex < contaminationCount; rowIndex++) {
                {
                    long fieldAddress = storageAddress + (bitOffset >>> 3);
                    int fieldShift = (int) bitOffset & 7;
                    long maxRadBits = doubleToRawLongBits(contamination.get(rowIndex).peak());
                    long maxTimeBits = contamination.get(rowIndex).lifetime();
                    long timeBits = contamination.get(rowIndex).remaining();
                    long ignoreArmorBits = contamination.get(rowIndex).bypassesArmor() ? 1L : 0L;
                    U.putLong(
                            storageBase,
                            fieldAddress,
                            partialByte & ((1L << fieldShift) - 1) | maxRadBits << fieldShift);
                    long maxTimeToTimeWord = (maxTimeBits & 0xFFFFFFFFL) | (timeBits << 32);
                    U.putLong(
                            storageBase,
                            fieldAddress + 8L,
                            maxRadBits >>> 1 >>> 63 - fieldShift | maxTimeToTimeWord << fieldShift);
                    long fieldWord =
                            maxTimeToTimeWord >>> 1 >>> 63 - fieldShift
                                    | ignoreArmorBits << fieldShift;
                    U.putByte(storageBase, fieldAddress + 16L, (byte) (fieldWord));
                    partialByte = fieldWord >>> (fieldShift + 1 & ~7);
                }
                bitOffset += 129L;
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
        if (bodyBytes < MIN_BODY_BYTES) throw new DecoderException("Invalid packet length");
        var player = context.playerOrNull();
        if (player == null) return;
        long bitOffset;
        long bodyLimit = storageAddress + bodyBytes;
        long radiationWord = U.getLong(storageBase, storageAddress);
        long radiationToDigammaWord = U.getLong(storageBase, storageAddress + 8L);
        long digammaToRadBufWord = U.getLong(storageBase, storageAddress + 16L);
        long radBufToBombTimerWord = U.getLong(storageBase, storageAddress + 24L);
        long bombTimerToBlacklungWord = U.getLong(storageBase, storageAddress + 32L);
        long blacklungToOilWord = U.getLong(storageBase, storageAddress + 37L);
        double radiation =
                longBitsToDouble(((radiationWord >>> 5) | (radiationToDigammaWord << 59)));
        double digamma =
                longBitsToDouble(((radiationToDigammaWord >>> 5) | (digammaToRadBufWord << 59)));
        double radBuf =
                longBitsToDouble(((digammaToRadBufWord >>> 5) | (radBufToBombTimerWord << 59)));
        int asbestos = (int) (radBufToBombTimerWord >>> 5);
        int bombTimer = (int) ((radBufToBombTimerWord >>> 37) | (bombTimerToBlacklungWord << 27));
        int contagion = (int) (bombTimerToBlacklungWord >>> 5);
        int blacklung =
                (int) ((bombTimerToBlacklungWord >>> 37) | ((blacklungToOilWord >>> 24) << 27));
        int oil = (int) (blacklungToOilWord >>> 29);
        if ((blacklungToOilWord >>> 61) != 0) throw new DecoderException("Nonzero packet padding");
        int contaminationCount =
                PacketValues.getVarInt(storageBase, storageAddress + 45L, bodyLimit);
        if ((contaminationCount & 0xFFFFFFFFL) > 2097151L)
            throw new DecoderException("Invalid collection count");
        long contaminationAddress =
                storageAddress + 45L + PacketValues.varIntSize(contaminationCount);
        if (((long) contaminationCount * 129 + 7) >>> 3 != bodyLimit - contaminationAddress)
            throw new DecoderException("Invalid packet tail length");
        LivingHazards hazards = LivingHazards.getOrCreate(player);
        hazards.clearDoses();
        bitOffset = (contaminationAddress - storageAddress) * 8;
        for (int rowIndex = 0; rowIndex < contaminationCount; rowIndex++) {
            long maxRadWord;
            long maxTimeToTimeWord;
            long ignoreArmorWord;
            {
                long fieldAddress = storageAddress + (bitOffset >>> 3);
                int fieldShift = (int) bitOffset & 7;
                long fieldLoad1 = U.getLong(storageBase, fieldAddress + 8L);
                long fieldLoad2 = (U.getByte(storageBase, fieldAddress + 16L) & 255L);
                maxRadWord =
                        U.getLong(storageBase, fieldAddress) >>> fieldShift
                                | fieldLoad1 << 1 << 63 - fieldShift;
                maxTimeToTimeWord = fieldLoad1 >>> fieldShift | fieldLoad2 << 1 << 63 - fieldShift;
                ignoreArmorWord = fieldLoad2 >>> fieldShift;
            }
            double maxRad = longBitsToDouble(maxRadWord);
            int maxTime = (int) maxTimeToTimeWord;
            int time = (int) (maxTimeToTimeWord >>> 32);
            boolean ignoreArmor = (ignoreArmorWord & 1L) != 0;
            bitOffset += 129L;
            hazards.addDose(new ContaminationEffect(maxRad, maxTime, time, ignoreArmor));
        }
        if ((bitOffset & 7) != 0
                && ((U.getByte(storageBase, storageAddress + (bitOffset >>> 3)) & 255)
                                >>> (int) (bitOffset & 7))
                        != 0) throw new DecoderException("Nonzero packet padding");
        ClientPacketEvents.hbmPlayerSync(
                player, radiation, digamma, radBuf, asbestos, bombTimer, contagion, blacklung, oil);
    }
}
