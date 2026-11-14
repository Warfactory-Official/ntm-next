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
import com.hbm.packet.sync.SyncPacketHandlers;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;

public final class BlueprintPoolSyncLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 0;
    public static final int MIN_BODY_BYTES = 1;
    public static final int MAX_BODY_BYTES = 1048576;

    private BlueprintPoolSyncLayout() {}

    public static EncodedPayload encode(Collection<String> pools) {
        int poolsCount = pools.size();
        if (poolsCount > 2097151) throw new IllegalArgumentException("Invalid pools count");
        EncodedPayload payload =
                EncodedPayload.allocateManaged(
                        PacketFamilyPrejoinBlueprintPools.TYPE, 5, MAX_BODY_BYTES, true, false);
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
            long poolsAddress = PacketValues.putVarInt(storageBase, storageAddress, poolsCount);
            encodedBuffer.writerIndex(bufferStart + (int) (poolsAddress - storageAddress));
            for (String pool : pools) {
                ByteBufCodecs.STRING_UTF8.encode(codecBuffer, pool);
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
        int poolsCount = PacketValues.getVarInt(storageBase, storageAddress, bodyLimit);
        if ((poolsCount & 0xFFFFFFFFL) > 2097151L)
            throw new DecoderException("Invalid collection count");
        long poolsAddress = storageAddress + PacketValues.varIntSize(poolsCount);
        List<String> pools = new ArrayList<>(poolsCount);
        decodedBuffer.readerIndex(bufferStart + (int) (poolsAddress - storageAddress));
        for (int elementIndex = 0; elementIndex < poolsCount; elementIndex++) {
            String pool = ByteBufCodecs.STRING_UTF8.decode(codecBuffer);
            pools.add(pool);
        }
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        SyncPacketHandlers.publishBlueprintPools(pools);
    }
}
