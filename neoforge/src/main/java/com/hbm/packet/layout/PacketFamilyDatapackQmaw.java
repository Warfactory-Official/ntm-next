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
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.platform.Network;
import com.hbm.qmaw.QMAWSyncCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;

public final class PacketFamilyDatapackQmaw {
    public static final Type<EncodedPayload> TYPE = new Type<>(Identifier.parse("hbm:dq"));
    public static final int MIN_BODY_BYTES = 1;
    public static final int MAX_BODY_BYTES = 67108864;
    public static final StreamCodec<ByteBuf, EncodedPayload> STREAM_CODEC =
            EncodedPayload.variableCodec(TYPE, MIN_BODY_BYTES, MAX_BODY_BYTES, true);

    private PacketFamilyDatapackQmaw() {}

    public static void register() {
        final CustomPacketPayload.Type<EncodedPayload> type = TYPE;
        final StreamCodec<RegistryFriendlyByteBuf, EncodedPayload> codec = STREAM_CODEC.cast();
        Network.datapackSync.add(
                new Network.DatapackSync<>(
                        type,
                        codec,
                        PacketFamilyDatapackQmaw::handle,
                        server -> QMAWSyncLayout.encode(QMAWSyncCodecs.snapshot(server))));
    }

    private static void handle(EncodedPayload payload) {
        try {
            receive(payload.buffer(), null);
        } finally {
            payload.close();
        }
    }

    public static void receive(ByteBuf decodedBuffer, PayloadHandlerContext context) {
        int bufferStart = decodedBuffer.readerIndex();
        int bodyBytes = decodedBuffer.readableBytes();
        Object storageBase;
        long storageAddress;
        if (decodedBuffer.hasArray()) {
            storageBase = decodedBuffer.array();
            storageAddress = BA_BASE + decodedBuffer.arrayOffset() + bufferStart;
        } else {
            storageBase = null;
            storageAddress = decodedBuffer.memoryAddress() + bufferStart;
        }
        QMAWSyncLayout.receiveFields(
                decodedBuffer, context, storageBase, storageAddress, bufferStart, bodyBytes);
    }
}
