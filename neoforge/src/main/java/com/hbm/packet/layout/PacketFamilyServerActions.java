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
import com.hbm.packet.PayloadHandlerThread;
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.platform.Network;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class PacketFamilyServerActions {
    public static final Type<EncodedPayload> TYPE = new Type<>(Identifier.parse("hbm:sa"));
    public static final int MIN_BODY_BYTES = 1;
    public static final int MAX_BODY_BYTES = 32767;
    public static final StreamCodec<ByteBuf, EncodedPayload> STREAM_CODEC =
            EncodedPayload.variableCodec(TYPE, MIN_BODY_BYTES, MAX_BODY_BYTES, false);

    private PacketFamilyServerActions() {}

    public static void register() {
        Network.serverbound.add(
                new Network.Registration<>(
                        TYPE,
                        STREAM_CODEC,
                        PacketFamilyServerActions::handle,
                        PayloadHandlerThread.MAIN));
    }

    private static void handle(EncodedPayload payload, PayloadHandlerContext context) {
        try {
            receive(payload.buffer(), context);
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
        switch (U.getByte(storageBase, storageAddress) & 31) {
            case 0 ->
                    ArmorFlightLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 1 ->
                    AnvilCraftLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 2 ->
                    BobmazonPurchaseLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 3 ->
                    ContainedStorageRequestLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 4 ->
                    CrateOpenHeldLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 5 ->
                    CraneKeyLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 6 ->
                    DesignatorCoordinatesLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 7 ->
                    FarViewLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 8 ->
                    FluidIdentifierLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 9 ->
                    FluidPipeTintRequestLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 10 ->
                    SatelliteCoordinatesLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 11 ->
                    KeybindLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 12 ->
                    PneumoAccessClickLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 13 ->
                    RadiationWatchLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 14 ->
                    RBMKDiagnosticsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 15 ->
                    RTTYPagerChannelLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 16 ->
                    SyncRequestLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 17 ->
                    ToolKeyLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 18 ->
                    ToolPresetsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            default -> throw new DecoderException("Unknown packet variant");
        }
    }
}
