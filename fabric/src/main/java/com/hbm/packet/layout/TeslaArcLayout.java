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
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

public final class TeslaArcLayout {
    public static final int VARIANT = 4;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 6;
    public static final int MAX_BODY_BYTES = 1048576;

    private TeslaArcLayout() {}

    public static EncodedPayload encode(int entityId, List<Vec3> targets) {
        int targetsCount = targets.size();
        if (targetsCount > 2097151) throw new IllegalArgumentException("Invalid targets count");
        long bodyBytesWide = 5L + PacketValues.varIntSize(targetsCount) + targetsCount * 24;
        if (bodyBytesWide > 1048576) throw new EncoderException("Packet exceeds frame limit");
        int bodyBytes = (int) bodyBytesWide;
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
            {
                long entityIdBits = entityId;
                U.putInt(storageBase, storageAddress, (int) (4L | (entityIdBits << 5)));
                U.putByte(storageBase, storageAddress + 4L, (byte) (((entityIdBits >>> 27) & 31L)));
            }
            long targetsAddress =
                    PacketValues.putVarInt(storageBase, storageAddress + 5L, targetsCount);
            for (int rowIndex = 0; rowIndex < targetsCount; rowIndex++) {
                {
                    long xBits = doubleToRawLongBits(targets.get(rowIndex).x);
                    long yBits = doubleToRawLongBits(targets.get(rowIndex).y);
                    long zBits = doubleToRawLongBits(targets.get(rowIndex).z);
                    U.putLong(storageBase, targetsAddress, xBits);
                    U.putLong(storageBase, targetsAddress + 8L, yBits);
                    U.putLong(storageBase, targetsAddress + 16L, zBits);
                }
                targetsAddress += 24L;
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
        long bodyLimit = storageAddress + bodyBytes;
        long entityIdWord = (U.getInt(storageBase, storageAddress) & 0xFFFFFFFFL);
        long entityIdWordPart2 = (U.getByte(storageBase, storageAddress + 4L) & 255L);
        int entityId = (int) ((entityIdWord >>> 5) | (entityIdWordPart2 << 27));
        if ((entityIdWordPart2 >>> 5) != 0) throw new DecoderException("Nonzero packet padding");
        int targetsCount = PacketValues.getVarInt(storageBase, storageAddress + 5L, bodyLimit);
        if ((targetsCount & 0xFFFFFFFFL) > 2097151L)
            throw new DecoderException("Invalid collection count");
        long targetsAddress = storageAddress + 5L + PacketValues.varIntSize(targetsCount);
        if ((long) targetsCount * 24 != bodyLimit - targetsAddress)
            throw new DecoderException("Invalid packet tail length");
        List<Vec3> targets = new ArrayList<>(targetsCount);
        for (int rowIndex = 0; rowIndex < targetsCount; rowIndex++) {
            long xWord = U.getLong(storageBase, targetsAddress);
            long yWord = U.getLong(storageBase, targetsAddress + 8L);
            long zWord = U.getLong(storageBase, targetsAddress + 16L);
            double x = longBitsToDouble(xWord);
            double y = longBitsToDouble(yWord);
            double z = longBitsToDouble(zWord);
            targets.add(new Vec3(x, y, z));
            targetsAddress += 24L;
        }
        ClientPacketEvents.teslaArc(entityId, targets);
    }
}
