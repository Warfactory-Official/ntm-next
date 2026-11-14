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
import com.hbm.packet.SyncUnitState;
import com.hbm.packet.codec.PacketValues;
import com.hbm.packet.sync.SyncPacketHandlers;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static net.minecraft.core.BlockPos.of;

public final class UnitSyncLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 2;
    public static final int MIN_BODY_BYTES = 12;
    public static final int MAX_BODY_BYTES = 1048576;

    private UnitSyncLayout() {}

    public static EncodedPayload encode(
            BlockPos pos,
            int blockEntityType,
            long revision,
            long base,
            SyncUnitState.Snapshot snapshot,
            long units) {
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilySyncBodies.TYPE, 34, MAX_BODY_BYTES, true, false, false);
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
                long posBits = pos.asLong();
                U.putLong(storageBase, storageAddress, (posBits << 2));
                U.putByte(storageBase, storageAddress + 8L, (byte) ((posBits >>> 62)));
            }
            long blockEntityTypeEnd =
                    PacketValues.putVarInt(storageBase, storageAddress + 9L, blockEntityType);
            long revisionEnd = PacketValues.putVarLong(storageBase, blockEntityTypeEnd, revision);
            long baseEnd = PacketValues.putVarLong(storageBase, revisionEnd, base);
            encodedBuffer.writerIndex(bufferStart + (int) (baseEnd - storageAddress));
            snapshot.writeBody(units, encodedBuffer);
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
        long posWord = U.getLong(storageBase, storageAddress);
        long posWordPart2 = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        BlockPos pos = of(((posWord >>> 2) | (posWordPart2 << 62)));
        if ((posWordPart2 >>> 2) != 0) throw new DecoderException("Nonzero packet padding");
        int blockEntityType = PacketValues.getVarInt(storageBase, storageAddress + 9L, bodyLimit);
        long blockEntityTypeEnd = storageAddress + 9L + PacketValues.varIntSize(blockEntityType);
        long revision = PacketValues.getVarLong(storageBase, blockEntityTypeEnd, bodyLimit);
        long revisionEnd = blockEntityTypeEnd + PacketValues.varLongSize(revision);
        long base = PacketValues.getVarLong(storageBase, revisionEnd, bodyLimit);
        long baseEnd = revisionEnd + PacketValues.varLongSize(base);
        decodedBuffer.readerIndex(bufferStart + (int) (baseEnd - storageAddress));
        ByteBuf snapshot = decodedBuffer;
        var player = context.playerOrNull();
        if (player == null) return;
        SyncPacketHandlers.applyUnits(player, pos, blockEntityType, revision, base, snapshot);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
    }

    static ClientboundBlockEntityDataPacket recordFields(
            ByteBuf decodedBuffer,
            Object storageBase,
            long storageAddress,
            int bufferStart,
            int bodyBytes) {
        if (bodyBytes < MIN_BODY_BYTES) throw new DecoderException("Invalid packet length");
        long bodyLimit = storageAddress + bodyBytes;
        long posWord = U.getLong(storageBase, storageAddress);
        long posWordPart2 = (U.getByte(storageBase, storageAddress + 8L) & 255L);
        BlockPos pos = of(((posWord >>> 2) | (posWordPart2 << 62)));
        if ((posWordPart2 >>> 2) != 0) throw new DecoderException("Nonzero packet padding");
        int blockEntityType = PacketValues.getVarInt(storageBase, storageAddress + 9L, bodyLimit);
        long blockEntityTypeEnd = storageAddress + 9L + PacketValues.varIntSize(blockEntityType);
        long revision = PacketValues.getVarLong(storageBase, blockEntityTypeEnd, bodyLimit);
        long revisionEnd = blockEntityTypeEnd + PacketValues.varLongSize(revision);
        long base = PacketValues.getVarLong(storageBase, revisionEnd, bodyLimit);
        long baseEnd = revisionEnd + PacketValues.varLongSize(base);
        decodedBuffer.readerIndex(bufferStart + (int) (baseEnd - storageAddress));
        ByteBuf snapshot = decodedBuffer;
        ClientboundBlockEntityDataPacket packet =
                SyncPacketHandlers.recordUnit(pos, blockEntityType, revision, base, snapshot);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        return packet;
    }
}
