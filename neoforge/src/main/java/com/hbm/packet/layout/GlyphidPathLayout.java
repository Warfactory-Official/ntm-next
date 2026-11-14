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
import com.hbm.packet.ClientEventConstants;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.world.level.pathfinder.Path;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

public final class GlyphidPathLayout {
    public static final int VARIANT = 5;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 22;
    public static final int MAX_BODY_BYTES = 2072;

    private GlyphidPathLayout() {}

    public static EncodedPayload encode(
            int entityId, int nextIndex, int targetX, int targetY, int targetZ, Path nodes) {
        int nodesCount =
                nodes == null
                        ? 0
                        : Math.min(
                                nodes.getNodeCount(), ClientEventConstants.GLYPHID_PATH_MAX_NODES);
        if (nodesCount > 128) throw new IllegalArgumentException("Invalid nodes count");
        int bodyBytes = 21 + PacketValues.varIntSize(nodesCount) + nodesCount * 16;
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
                long nextIndexBits = nextIndex;
                long targetXBits = targetX;
                long targetYBits = targetY;
                long targetZBits = targetZ;
                U.putLong(
                        storageBase,
                        storageAddress,
                        5L | ((entityIdBits & 0xFFFFFFFFL) << 5) | (nextIndexBits << 37));
                U.putLong(
                        storageBase,
                        storageAddress + 8L,
                        ((nextIndexBits >>> 27) & 31L)
                                | ((targetXBits & 0xFFFFFFFFL) << 5)
                                | (targetYBits << 37));
                U.putLong(
                        storageBase,
                        storageAddress + 13L,
                        ((targetYBits >>> 3) & 0x1FFFFFFFL) | ((targetZBits & 0xFFFFFFFFL) << 29));
            }
            long nodesAddress =
                    PacketValues.putVarInt(storageBase, storageAddress + 21L, nodesCount);
            for (int rowIndex = 0; rowIndex < nodesCount; rowIndex++) {
                {
                    long nodeXBits = nodes.getNode(rowIndex).x;
                    long nodeYBits = nodes.getNode(rowIndex).y;
                    long nodeZBits = nodes.getNode(rowIndex).z;
                    long costBits = floatToRawIntBits(nodes.getNode(rowIndex).f);
                    U.putLong(
                            storageBase,
                            nodesAddress,
                            (nodeXBits & 0xFFFFFFFFL) | (nodeYBits << 32));
                    U.putLong(
                            storageBase,
                            nodesAddress + 8L,
                            (nodeZBits & 0xFFFFFFFFL) | (costBits << 32));
                }
                nodesAddress += 16L;
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
        long entityIdToNextIndexWord = U.getLong(storageBase, storageAddress);
        long nextIndexToTargetYWord = U.getLong(storageBase, storageAddress + 8L);
        long targetYToTargetZWord = U.getLong(storageBase, storageAddress + 13L);
        int entityId = (int) (entityIdToNextIndexWord >>> 5);
        int nextIndex = (int) ((entityIdToNextIndexWord >>> 37) | (nextIndexToTargetYWord << 27));
        int targetX = (int) (nextIndexToTargetYWord >>> 5);
        int targetY =
                (int) ((nextIndexToTargetYWord >>> 37) | ((targetYToTargetZWord >>> 24) << 27));
        int targetZ = (int) (targetYToTargetZWord >>> 29);
        if ((targetYToTargetZWord >>> 61) != 0)
            throw new DecoderException("Nonzero packet padding");
        int nodesCount = PacketValues.getVarInt(storageBase, storageAddress + 21L, bodyLimit);
        if ((nodesCount & 0xFFFFFFFFL) > 128L)
            throw new DecoderException("Invalid collection count");
        long nodesAddress = storageAddress + 21L + PacketValues.varIntSize(nodesCount);
        if ((long) nodesCount * 16 != bodyLimit - nodesAddress)
            throw new DecoderException("Invalid packet tail length");
        int[] nodes = new int[nodesCount * 3];
        float[] costs = new float[nodesCount];
        for (int rowIndex = 0; rowIndex < nodesCount; rowIndex++) {
            long nodeXToNodeYWord = U.getLong(storageBase, nodesAddress);
            long nodeZToCostWord = U.getLong(storageBase, nodesAddress + 8L);
            int nodeX = (int) nodeXToNodeYWord;
            int nodeY = (int) (nodeXToNodeYWord >>> 32);
            int nodeZ = (int) nodeZToCostWord;
            float cost = intBitsToFloat((int) (nodeZToCostWord >>> 32));
            nodes[3 * rowIndex] = nodeX;
            nodes[3 * rowIndex + 1] = nodeY;
            nodes[3 * rowIndex + 2] = nodeZ;
            costs[rowIndex] = cost;
            nodesAddress += 16L;
        }
        ClientPacketEvents.glyphidPath(
                entityId, nextIndex, targetX, targetY, targetZ, nodes, costs);
    }
}
