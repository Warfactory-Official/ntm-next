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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.JA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class FallingMultiblockLayout {
    public static final int VARIANT = 26;
    public static final int VARIANT_BITS = 5;
    public static final int MIN_BODY_BYTES = 15;
    public static final int MAX_BODY_BYTES = 1048576;

    private FallingMultiblockLayout() {}

    public static EncodedPayload encode(
            int entityId,
            long corePos,
            long[] members,
            boolean carriedPresent,
            CompoundTag carried) {
        int membersCount = members.length;
        if (membersCount > 2097151) throw new IllegalArgumentException("Invalid members count");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyClientEvents.TYPE,
                        Math.max(
                                MIN_BODY_BYTES,
                                (int) ((145L + (long) membersCount * 64L + 7L) >>> 3)),
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
            ByteBuf codecBuffer = encodedBuffer;
            {
                long entityIdBits = entityId;
                long corePosBits = corePos;
                U.putLong(
                        storageBase,
                        storageAddress,
                        26L | ((entityIdBits & 0xFFFFFFFFL) << 5) | (corePosBits << 37));
                U.putLong(storageBase, storageAddress + 5L, (corePosBits >>> 3));
            }
            long membersAddress =
                    PacketValues.putVarInt(storageBase, storageAddress + 13L, membersCount);
            U.copyMemory(members, JA_BASE, storageBase, membersAddress, (long) membersCount * 8);
            long membersEnd = membersAddress + (long) membersCount * 8;
            {
                long carriedPresentBits = carriedPresent ? 1L : 0L;
                U.putByte(storageBase, membersEnd, (byte) (carriedPresentBits));
            }
            encodedBuffer.writerIndex(bufferStart + (int) (membersEnd - storageAddress) + 1);
            if (carriedPresent) {
                ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(codecBuffer, carried);
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
        ByteBuf codecBuffer = decodedBuffer;
        long bodyLimit = storageAddress + bodyBytes;
        long entityIdToCorePosWord = U.getLong(storageBase, storageAddress);
        long corePosWord = U.getLong(storageBase, storageAddress + 5L);
        int entityId = (int) (entityIdToCorePosWord >>> 5);
        long corePos = ((entityIdToCorePosWord >>> 37) | ((corePosWord >>> 24) << 27));
        if ((corePosWord >>> 61) != 0) throw new DecoderException("Nonzero packet padding");
        int membersCount = PacketValues.getVarInt(storageBase, storageAddress + 13L, bodyLimit);
        if ((membersCount & 0xFFFFFFFFL) > 2097151L)
            throw new DecoderException("Invalid collection count");
        long membersAddress = storageAddress + 13L + PacketValues.varIntSize(membersCount);
        if (bodyLimit - membersAddress < (long) membersCount * 8)
            throw new DecoderException("Truncated packet collection");
        long[] members = ClientPacketEvents.fallingMultiblockMembers(entityId, membersCount);
        U.copyMemory(storageBase, membersAddress, members, JA_BASE, (long) membersCount * 8);
        long membersEnd = membersAddress + (long) membersCount * 8;
        if (bodyLimit - membersEnd < 1) throw new DecoderException("Truncated packet fields");
        long carriedPresentWord = (U.getByte(storageBase, membersEnd) & 255L);
        boolean carriedPresent = (carriedPresentWord & 1L) != 0;
        if ((carriedPresentWord >>> 1) != 0) throw new DecoderException("Nonzero packet padding");
        decodedBuffer.readerIndex(bufferStart + (int) (membersEnd - storageAddress) + 1);
        CompoundTag carried = null;
        if (carriedPresent) {
            carried = ByteBufCodecs.TRUSTED_COMPOUND_TAG.decode(codecBuffer);
        }
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        ClientPacketEvents.fallingMultiblock(entityId, corePos, members, carriedPresent, carried);
    }
}
