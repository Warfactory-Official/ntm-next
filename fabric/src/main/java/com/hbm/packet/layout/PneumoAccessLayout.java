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

import com.hbm.inventory.container.PneumoStorageAccessMenu;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketBuffers;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.world.item.ItemStack;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class PneumoAccessLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 0;
    public static final int MIN_BODY_BYTES = 2;
    public static final int MAX_BODY_BYTES = 1048576;

    private PneumoAccessLayout() {}

    public static EncodedPayload encode(
            int containerId, List<PneumoStorageAccessMenu.Delta> deltas, int firstDelta) {
        int deltasCount =
                Math.min(deltas.size() - firstDelta, PneumoStorageAccessMenu.DELTAS_PER_MESSAGE);
        if (deltasCount > 48) throw new IllegalArgumentException("Invalid deltas count");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilySyncRegistry.TYPE,
                        Math.max(
                                MIN_BODY_BYTES,
                                (int)
                                        ((80L
                                                        + (long) deltasCount * 1L
                                                        + (long) deltasCount * 134L
                                                        + 7L)
                                                >>> 3)),
                        MAX_BODY_BYTES,
                        true,
                        false,
                        true);
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
            long bitOffset;
            long partialByte = 0L;
            long containerIdEnd = PacketValues.putVarInt(storageBase, storageAddress, containerId);
            long deltasAddress = PacketValues.putVarInt(storageBase, containerIdEnd, deltasCount);
            bitOffset = (deltasAddress - storageAddress) * 8;
            for (int rowIndex = 0; rowIndex < deltasCount; rowIndex++) {
                {
                    long fieldAddress = storageAddress + (bitOffset >>> 3);
                    int fieldShift = (int) bitOffset & 7;
                    long displayPresentBits =
                            deltas.get(firstDelta + rowIndex).display() != null ? 1L : 0L;
                    long fieldWord =
                            partialByte & ((1L << fieldShift) - 1)
                                    | displayPresentBits << fieldShift;
                    U.putByte(storageBase, fieldAddress, (byte) (fieldWord));
                    partialByte = fieldWord >>> (fieldShift + 1 & ~7);
                }
                bitOffset += 1L;
            }
            bitOffset = (bitOffset + 7L) & ~7L;
            long rowAddress = storageAddress + (bitOffset >>> 3);
            for (int rowIndex = 0; rowIndex < deltasCount; rowIndex++) {
                rowAddress =
                        PacketValues.putVarInt(
                                storageBase, rowAddress, deltas.get(firstDelta + rowIndex).id());
                rowAddress =
                        PacketValues.putVarLong(
                                storageBase,
                                rowAddress,
                                deltas.get(firstDelta + rowIndex).amount());
            }
            encodedBuffer.writerIndex(bufferStart + (int) (rowAddress - storageAddress));
            for (int rowIndex = 0; rowIndex < deltasCount; rowIndex++) {
                if (deltas.get(firstDelta + rowIndex).display() != null) {
                    ItemStack.STREAM_CODEC.encode(
                            codecBuffer, deltas.get(firstDelta + rowIndex).display());
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
        var player = context.playerOrNull();
        if (player == null) return;
        var codecBuffer = PacketBuffers.registry(decodedBuffer, context);
        long bitOffset;
        long availableBits = (long) bodyBytes * 8;
        long bodyLimit = storageAddress + bodyBytes;
        int containerId = PacketValues.getVarInt(storageBase, storageAddress, bodyLimit);
        long containerIdEnd = storageAddress + PacketValues.varIntSize(containerId);
        int deltasCount = PacketValues.getVarInt(storageBase, containerIdEnd, bodyLimit);
        if ((deltasCount & 0xFFFFFFFFL) > 48L)
            throw new DecoderException("Invalid collection count");
        long deltasAddress = containerIdEnd + PacketValues.varIntSize(deltasCount);
        long deltasRowBits = (deltasAddress - storageAddress) * 8;
        if (bodyLimit - deltasAddress < ((long) deltasCount * 1 + 7) >>> 3)
            throw new DecoderException("Truncated packet collection");
        bitOffset = (deltasAddress - storageAddress) * 8;
        bitOffset += (long) deltasCount * 1L;
        if ((bitOffset & 7) != 0
                && ((U.getByte(storageBase, storageAddress + (bitOffset >>> 3)) & 255)
                                >>> (int) (bitOffset & 7))
                        != 0) throw new DecoderException("Nonzero packet padding");
        bitOffset = (bitOffset + 7L) & ~7L;
        long deltasRows = storageAddress + (bitOffset >>> 3);
        if ((long) deltasCount * 16 > availableBits - bitOffset)
            throw new DecoderException("Truncated packet collection");
        long rowAddress = storageAddress + (bitOffset >>> 3);
        for (int rowIndex = 0; rowIndex < deltasCount; rowIndex++) {
            int id = PacketValues.getVarInt(storageBase, rowAddress, bodyLimit);
            rowAddress = rowAddress + PacketValues.varIntSize(id);
            long amount = PacketValues.getVarLong(storageBase, rowAddress, bodyLimit);
            rowAddress = rowAddress + PacketValues.varLongSize(amount);
        }
        decodedBuffer.readerIndex(bufferStart + (int) (rowAddress - storageAddress));
        bitOffset = deltasRowBits;
        for (int rowIndex = 0; rowIndex < deltasCount; rowIndex++) {
            long displayPresentWord;
            {
                long fieldAddress = storageAddress + (bitOffset >>> 3);
                int fieldShift = (int) bitOffset & 7;
                displayPresentWord = (U.getByte(storageBase, fieldAddress) & 255L) >>> fieldShift;
            }
            boolean displayPresent = (displayPresentWord & 1L) != 0;
            bitOffset += 1L;
            int id = PacketValues.getVarInt(storageBase, deltasRows, bodyLimit);
            deltasRows = deltasRows + PacketValues.varIntSize(id);
            long amount = PacketValues.getVarLong(storageBase, deltasRows, bodyLimit);
            deltasRows = deltasRows + PacketValues.varLongSize(amount);
            ItemStack display = null;
            if (displayPresent) {
                display = ItemStack.STREAM_CODEC.decode(codecBuffer);
            }
            PneumoStorageAccessMenu.applyDelta(
                    player, containerId, id, displayPresent ? display : null, amount);
        }
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        PneumoStorageAccessMenu.applySync(player, containerId);
    }
}
