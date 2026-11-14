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
import com.hbm.entity.item.FallingBlockBatchEntity;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

public final class FallingBlockBatchLayout {
    public static final int VARIANT = 27;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 10;
    public static final int MAX_BODY_BYTES = 73740;

    public interface Children {
        int count();

        int id(int index);

        long source(int index);

        int stateId(int index);

        double y(int index);

        double velocity(int index);

        int age(int index);
    }

    private FallingBlockBatchLayout() {}

    public static EncodedPayload encode(
            int entityId, int revision, boolean running, Children children) {
        int childrenCount = children.count();
        if (childrenCount > 2048) throw new IllegalArgumentException("Invalid children count");
        int bodyBytes = 9 + PacketValues.varIntSize(childrenCount) + childrenCount * 36;
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
                long revisionBits = revision;
                long runningBits = running ? 1L : 0L;
                U.putLong(
                        storageBase,
                        storageAddress,
                        27L | ((entityIdBits & 0xFFFFFFFFL) << 5) | (revisionBits << 37));
                U.putByte(
                        storageBase,
                        storageAddress + 8L,
                        (byte) (((revisionBits >>> 27) & 31L) | (runningBits << 5)));
            }
            long childrenAddress =
                    PacketValues.putVarInt(storageBase, storageAddress + 9L, childrenCount);
            for (int rowIndex = 0; rowIndex < childrenCount; rowIndex++) {
                {
                    long idBits = children.id(rowIndex);
                    long sourceBits = children.source(rowIndex);
                    long stateIdBits = children.stateId(rowIndex);
                    long yBits = doubleToRawLongBits(children.y(rowIndex));
                    long velocityBits = doubleToRawLongBits(children.velocity(rowIndex));
                    long ageBits = children.age(rowIndex);
                    U.putLong(
                            storageBase,
                            childrenAddress,
                            (idBits & 0xFFFFFFFFL) | (sourceBits << 32));
                    U.putLong(
                            storageBase,
                            childrenAddress + 8L,
                            (sourceBits >>> 32) | (stateIdBits << 32));
                    U.putLong(storageBase, childrenAddress + 16L, yBits);
                    U.putLong(storageBase, childrenAddress + 24L, velocityBits);
                    U.putInt(storageBase, childrenAddress + 32L, (int) (ageBits));
                }
                childrenAddress += 36L;
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
        long entityIdToRevisionWord = U.getLong(storageBase, storageAddress);
        long revisionToRunningWord = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        int entityId = (int) (entityIdToRevisionWord >>> 5);
        int revision = (int) ((entityIdToRevisionWord >>> 37) | (revisionToRunningWord << 27));
        boolean running = ((revisionToRunningWord >>> 5) & 1L) != 0;
        if ((revisionToRunningWord >>> 6) != 0)
            throw new DecoderException("Nonzero packet padding");
        int childrenCount = PacketValues.getVarInt(storageBase, storageAddress + 9L, bodyLimit);
        if ((childrenCount & 0xFFFFFFFFL) > 2048L)
            throw new DecoderException("Invalid collection count");
        long childrenAddress = storageAddress + 9L + PacketValues.varIntSize(childrenCount);
        if ((long) childrenCount * 36 != bodyLimit - childrenAddress)
            throw new DecoderException("Invalid packet tail length");
        FallingBlockBatchEntity children = ClientPacketEvents.fallingBlockBatch(entityId, revision);
        for (int rowIndex = 0; rowIndex < childrenCount; rowIndex++) {
            long idToSourceWord = U.getLong(storageBase, childrenAddress);
            long sourceToStateIdWord = U.getLong(storageBase, childrenAddress + 8L);
            long yWord = U.getLong(storageBase, childrenAddress + 16L);
            long velocityWord = U.getLong(storageBase, childrenAddress + 24L);
            long ageWord = (U.getInt(storageBase, childrenAddress + 32L) & 0xFFFFFFFFL);
            int id = (int) idToSourceWord;
            long source = ((idToSourceWord >>> 32) | (sourceToStateIdWord << 32));
            int stateId = (int) (sourceToStateIdWord >>> 32);
            double y = longBitsToDouble(yWord);
            double velocity = longBitsToDouble(velocityWord);
            int age = (int) ageWord;
            if (children != null)
                children.acceptNetworkChild(id, source, stateId, y, velocity, age);
            childrenAddress += 36L;
        }
        ClientPacketEvents.fallingBlockBatchApplied(entityId, revision, running, children);
    }
}
