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

import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.world.bulk.BulkPasteSessions;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class BulkPasteLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 0;
    public static final int MIN_BODY_BYTES = 10;
    public static final int MAX_BODY_BYTES = 2147483647;

    public interface Blocks {
        int count();

        long position(int index);

        int stateId(int index);

        boolean tagPresent(int index);

        CompoundTag tag(int index);
    }

    private BulkPasteLayout() {}

    public static EncodedPayload encode(long session, boolean finished, Blocks blocks) {
        int blocksCount = blocks.count();
        if (blocksCount > 256) throw new IllegalArgumentException("Invalid blocks count");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyBulkPaste.TYPE,
                        Math.max(
                                MIN_BODY_BYTES,
                                (int)
                                        ((112L
                                                        + (long) blocksCount * 65L
                                                        + (long) blocksCount * 47L
                                                        + 7L)
                                                >>> 3)),
                        MAX_BODY_BYTES,
                        false,
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
            ByteBuf codecBuffer = encodedBuffer;
            long bitOffset;
            long partialByte = 0L;
            {
                long sessionBits = session;
                long finishedBits = finished ? 1L : 0L;
                U.putLong(storageBase, storageAddress, sessionBits);
                U.putByte(storageBase, storageAddress + 8L, (byte) (finishedBits));
            }
            long blocksAddress =
                    PacketValues.putVarInt(storageBase, storageAddress + 9L, blocksCount);
            bitOffset = (blocksAddress - storageAddress) * 8;
            for (int rowIndex = 0; rowIndex < blocksCount; rowIndex++) {
                {
                    long fieldAddress = storageAddress + (bitOffset >>> 3);
                    int fieldShift = (int) bitOffset & 7;
                    long positionBits = blocks.position(rowIndex);
                    long tagPresentBits = blocks.tagPresent(rowIndex) ? 1L : 0L;
                    U.putLong(
                            storageBase,
                            fieldAddress,
                            partialByte & ((1L << fieldShift) - 1) | positionBits << fieldShift);
                    long fieldWord =
                            positionBits >>> 1 >>> 63 - fieldShift | tagPresentBits << fieldShift;
                    U.putByte(storageBase, fieldAddress + 8L, (byte) (fieldWord));
                    partialByte = fieldWord >>> (fieldShift + 1 & ~7);
                }
                bitOffset += 65L;
            }
            bitOffset = (bitOffset + 7L) & ~7L;
            long rowAddress = storageAddress + (bitOffset >>> 3);
            for (int rowIndex = 0; rowIndex < blocksCount; rowIndex++) {
                rowAddress =
                        PacketValues.putVarInt(storageBase, rowAddress, blocks.stateId(rowIndex));
            }
            encodedBuffer.writerIndex(bufferStart + (int) (rowAddress - storageAddress));
            for (int rowIndex = 0; rowIndex < blocksCount; rowIndex++) {
                if (blocks.tagPresent(rowIndex)) {
                    ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(codecBuffer, blocks.tag(rowIndex));
                }
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
        long bitOffset;
        long availableBits = (long) bodyBytes * 8;
        long bodyLimit = storageAddress + bodyBytes;
        long sessionWord = U.getLong(storageBase, storageAddress);
        long finishedWord = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        long session = sessionWord;
        boolean finished = (finishedWord & 1L) != 0;
        if ((finishedWord >>> 1) != 0) throw new DecoderException("Nonzero packet padding");
        int blocksCount = PacketValues.getVarInt(storageBase, storageAddress + 9L, bodyLimit);
        if ((blocksCount & 0xFFFFFFFFL) > 256L)
            throw new DecoderException("Invalid collection count");
        long blocksAddress = storageAddress + 9L + PacketValues.varIntSize(blocksCount);
        long blocksRowBits = (blocksAddress - storageAddress) * 8;
        if (bodyLimit - blocksAddress < ((long) blocksCount * 65 + 7) >>> 3)
            throw new DecoderException("Truncated packet collection");
        bitOffset = (blocksAddress - storageAddress) * 8;
        bitOffset += (long) blocksCount * 65L;
        if ((bitOffset & 7) != 0
                && ((U.getByte(storageBase, storageAddress + (bitOffset >>> 3)) & 255)
                                >>> (int) (bitOffset & 7))
                        != 0) throw new DecoderException("Nonzero packet padding");
        bitOffset = (bitOffset + 7L) & ~7L;
        long blocksRows = storageAddress + (bitOffset >>> 3);
        if ((long) blocksCount * 8 > availableBits - bitOffset)
            throw new DecoderException("Truncated packet collection");
        long rowAddress = storageAddress + (bitOffset >>> 3);
        for (int rowIndex = 0; rowIndex < blocksCount; rowIndex++) {
            int stateId = PacketValues.getVarInt(storageBase, rowAddress, bodyLimit);
            rowAddress = rowAddress + PacketValues.varIntSize(stateId);
        }
        long[] positions = new long[blocksCount];
        BlockState[] states = new BlockState[blocksCount];
        CompoundTag[] tags = new CompoundTag[blocksCount];
        decodedBuffer.readerIndex(bufferStart + (int) (rowAddress - storageAddress));
        bitOffset = blocksRowBits;
        for (int rowIndex = 0; rowIndex < blocksCount; rowIndex++) {
            long positionWord;
            long tagPresentWord;
            {
                long fieldAddress = storageAddress + (bitOffset >>> 3);
                int fieldShift = (int) bitOffset & 7;
                long fieldLoad1 = (U.getByte(storageBase, fieldAddress + 8L) & 255L);
                positionWord =
                        U.getLong(storageBase, fieldAddress) >>> fieldShift
                                | fieldLoad1 << 1 << 63 - fieldShift;
                tagPresentWord = fieldLoad1 >>> fieldShift;
            }
            long position = positionWord;
            boolean tagPresent = (tagPresentWord & 1L) != 0;
            bitOffset += 65L;
            int stateId = PacketValues.getVarInt(storageBase, blocksRows, bodyLimit);
            blocksRows = blocksRows + PacketValues.varIntSize(stateId);
            CompoundTag tag = null;
            if (tagPresent) {
                tag = ByteBufCodecs.TRUSTED_COMPOUND_TAG.decode(codecBuffer);
            }
            positions[rowIndex] = position;
            states[rowIndex] = Block.stateById(stateId);
            tags[rowIndex] = tagPresent ? tag : null;
        }
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        BulkPasteSessions.paste(player, session, finished, positions, states, tags);
    }
}
