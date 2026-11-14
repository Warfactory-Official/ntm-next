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

package com.hbm.mixin.compat.network.neb;

import cn.ussshenzhou.notenoughbandwidth.aggregation.PacketAggregationPacket;
import com.hbm.packet.compat.NebCalls;
import com.hbm.packet.compat.NebEntry;
import com.hbm.packet.threading.ThreadedPayload;
import java.util.ArrayList;
import net.minecraft.network.Connection;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;

@Mixin(value = PacketAggregationPacket.class, remap = false)
public abstract class MixinPacketAggregationPacket {
    @Shadow @Final private ArrayList<?> packetsToEncode;
    @Shadow @Final private ProtocolInfo<?> protocolInfo;
    @Shadow private Connection connection;
    @Shadow private int bakedSize, innerBlobSize;
    @Shadow @Final private static Logger LOGGER;

    @Overwrite
    public void write(RegistryFriendlyByteBuf buffer) {
        int start = buffer.writerIndex();
        var scratch = connection;
        RegistryFriendlyByteBuf raw =
                ((com.hbm.packet.compat.NebBuffers) (scratch)).hbm$raw(buffer.registryAccess());
        int count = packetsToEncode.size();
        int[] sizes = ((com.hbm.packet.compat.NebBuffers) (scratch)).hbm$sizes(count);
        boolean sampling = NebCalls.sampling();
        int previous = 0;
        for (int i = 0; i < count; i++) {
            NebEntry packet = (NebEntry) packetsToEncode.get(i);
            NebCalls.prefix(packet.hbm$type(), raw);
            if (packet.hbm$payload() instanceof ThreadedPayload payload) {
                raw.writeVarInt(payload.managedLength());
                int uncompressedSize = raw.writerIndex() + payload.managedLength();
                if (count == 1 && uncompressedSize < 32 && !sampling) {
                    buffer.writeBoolean(false);
                    buffer.writeBytes(raw, 0, raw.writerIndex());
                    payload.writeManagedBody(buffer);
                    bakedSize = uncompressedSize;
                    innerBlobSize = buffer.writerIndex() - start;
                    NebCalls.out(uncompressedSize);
                    NebCalls.stats(
                            protocolInfo.flow(),
                            packet.hbm$type(),
                            uncompressedSize,
                            innerBlobSize);
                    return;
                }
                payload.writeManagedBody(raw);
            } else {
                RegistryFriendlyByteBuf encoded =
                        ((com.hbm.packet.compat.NebBuffers) (scratch))
                                .hbm$packet(buffer.registryAccess());
                packet.hbm$encode(encoded, protocolInfo, protocolInfo.flow());
                raw.writeVarInt(encoded.readableBytes());
                raw.writeBytes(encoded, encoded.readerIndex(), encoded.readableBytes());
            }
            sizes[i] = raw.writerIndex() - previous;
            previous = raw.writerIndex();
        }
        int rawSize = raw.readableBytes();
        if (sampling) {
            byte[] sample = new byte[rawSize];
            raw.getBytes(raw.readerIndex(), sample);
            NebCalls.sample(sample);
        }
        boolean compress = rawSize >= 32;
        buffer.writeBoolean(compress);
        if (compress) {
            buffer.writeVarInt(rawSize);
            int compressedStart = buffer.writerIndex();
            NebCalls.context(connection).hbm$compress(raw, buffer);
            bakedSize = buffer.writerIndex() - compressedStart;
            if (NebCalls.debug())
                LOGGER.debug(
                        "Aggregated and compressed: {} -> {} bytes ({} %)",
                        rawSize, bakedSize, String.format("%.2f", 100f * bakedSize / rawSize));
        } else {
            buffer.writeBytes(raw, raw.readerIndex(), rawSize);
            bakedSize = rawSize;
        }
        NebCalls.out(rawSize);
        innerBlobSize = buffer.writerIndex() - start;
        if (rawSize <= 0) return;
        long allocated = 0;
        for (int i = 0; i < count; i++) {
            long encoded =
                    i == count - 1
                            ? Math.max(0L, innerBlobSize - allocated)
                            : (long) Math.floor((double) sizes[i] * innerBlobSize / rawSize);
            allocated += encoded;
            NebCalls.stats(
                    protocolInfo.flow(),
                    ((NebEntry) packetsToEncode.get(i)).hbm$type(),
                    sizes[i],
                    encoded);
        }
    }
}
