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

import com.hbm.items.machine.PwrPrintData;
import com.hbm.items.machine.PwrPrintSyncHandlers;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static net.minecraft.core.BlockPos.of;

public final class PwrPrintLayout {
    public static final int VARIANT = 4;
    public static final int VARIANT_BITS = 3;
    public static final int MIN_BODY_BYTES = 18;
    public static final int MAX_BODY_BYTES = 1048576;
    private static final Direction[] directionValues = Direction.values();

    static {
        if (directionValues.length != 6)
            throw new ExceptionInInitializerError("Enum table size changed: direction");
    }

    private PwrPrintLayout() {}

    public static EncodedPayload encode(
            BlockPos min, BlockPos max, Direction direction, PwrPrintData cells) {
        int cellsCount = cells.positions().length;
        if (cellsCount > 2097151) throw new IllegalArgumentException("Invalid cells count");
        long bodyBits = 136L + 8L * (PacketValues.varIntSize(cellsCount));
        for (int rowIndex = 0; rowIndex < cellsCount; rowIndex++) {
            bodyBits += 64L;
            bodyBits +=
                    8L
                            * PacketValues.varIntSize(
                                    Block.BLOCK_STATE_REGISTRY.getId(cells.states()[rowIndex]));
        }
        if (bodyBits > 8388608L) throw new EncoderException("Packet exceeds frame limit");
        int bodyBytes = (int) ((bodyBits + 7L) >>> 3);
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilySyncValues.TYPE, bodyBytes, MAX_BODY_BYTES, true, false, false);
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
                long minBits = min.asLong();
                long maxBits = max.asLong();
                long directionBits = direction.ordinal();
                U.putLong(storageBase, storageAddress, 4L | (minBits << 3));
                U.putLong(storageBase, storageAddress + 8L, (minBits >>> 61) | (maxBits << 3));
                U.putByte(
                        storageBase,
                        storageAddress + 16L,
                        (byte) ((maxBits >>> 61) | (directionBits << 3)));
            }
            long cellsAddress =
                    PacketValues.putVarInt(storageBase, storageAddress + 17L, cellsCount);
            for (int rowIndex = 0; rowIndex < cellsCount; rowIndex++) {
                {
                    long positionBits = cells.positions()[rowIndex];
                    U.putLong(storageBase, cellsAddress, positionBits);
                }
                cellsAddress =
                        PacketValues.putVarInt(
                                storageBase,
                                cellsAddress + 8L,
                                Block.BLOCK_STATE_REGISTRY.getId(cells.states()[rowIndex]));
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
        long minWord = U.getLong(storageBase, storageAddress);
        long minToMaxWord = U.getLong(storageBase, storageAddress + 8L);
        long maxToDirectionWord = (U.getByte(storageBase, storageAddress + 16L) & 255L);
        BlockPos min = of(((minWord >>> 3) | (minToMaxWord << 61)));
        BlockPos max = of(((minToMaxWord >>> 3) | (maxToDirectionWord << 61)));
        long directionBits = ((maxToDirectionWord >>> 3) & 7L);
        if (directionBits >= 6L) throw new DecoderException("Invalid direction ordinal");
        Direction direction = directionValues[(int) directionBits];
        if ((maxToDirectionWord >>> 6) != 0) throw new DecoderException("Nonzero packet padding");
        int cellsCount = PacketValues.getVarInt(storageBase, storageAddress + 17L, bodyLimit);
        if ((cellsCount & 0xFFFFFFFFL) > 2097151L)
            throw new DecoderException("Invalid collection count");
        long cellsAddress = storageAddress + 17L + PacketValues.varIntSize(cellsCount);
        if (bodyLimit - cellsAddress < (long) cellsCount * 9)
            throw new DecoderException("Truncated packet collection");
        long[] positions = new long[cellsCount];
        BlockState[] states = new BlockState[cellsCount];
        for (int rowIndex = 0; rowIndex < cellsCount; rowIndex++) {
            if (bodyLimit - cellsAddress < 8) throw new DecoderException("Truncated packet fields");
            long positionWord = U.getLong(storageBase, cellsAddress);
            long position = positionWord;
            int stateId = PacketValues.getVarInt(storageBase, cellsAddress + 8L, bodyLimit);
            cellsAddress = cellsAddress + 8L + PacketValues.varIntSize(stateId);
            positions[rowIndex] = position;
            states[rowIndex] = PwrPrintSyncHandlers.state(stateId);
        }
        if (bodyLimit != cellsAddress) throw new DecoderException("Unexpected packet tail");
        PwrPrintSyncHandlers.open(min, max, direction, positions, states);
    }
}
