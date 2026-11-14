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

public final class PacketFamilyClientEvents {
    public static final Type<EncodedPayload> TYPE = new Type<>(Identifier.parse("hbm:ce"));
    public static final int MIN_BODY_BYTES = 1;
    public static final int MAX_BODY_BYTES = 1048576;
    public static final StreamCodec<ByteBuf, EncodedPayload> STREAM_CODEC =
            EncodedPayload.variableCodec(TYPE, MIN_BODY_BYTES, MAX_BODY_BYTES, false);

    private PacketFamilyClientEvents() {}

    public static void register() {
        Network.clientbound.add(
                new Network.Registration<>(
                        TYPE,
                        STREAM_CODEC,
                        PacketFamilyClientEvents::handle,
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
                    GunAnimationLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 1 ->
                    GunSoundLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 2 ->
                    GrenadeAnimationLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 3 ->
                    MuzzleFlashLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 4 ->
                    TeslaArcLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 5 ->
                    GlyphidPathLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 6 ->
                    GasFlameLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 7 ->
                    JetpackDnsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 8 ->
                    JetpackBjLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 9 ->
                    JetpackRegularLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 10 ->
                    JetpackVectorLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 11 ->
                    EffectRadFogLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 12 ->
                    EffectExplosionSmallLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 13 ->
                    EffectExplosionStandardLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 14 ->
                    EffectExplosionLargeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 15 ->
                    RBMKMushLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 16 ->
                    EffectGasFlameLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 17 ->
                    EffectGasFlameFlareLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 18 ->
                    EffectHadronLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 19 ->
                    EffectAnnihilatorFlameLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 20 ->
                    EffectGasFlameVentLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 21 ->
                    HbmPlayerSyncLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 22 ->
                    ImpactSyncLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 23 ->
                    PollutionSyncLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 24 ->
                    PlayerAppearanceLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 25 ->
                    OcclusionRefreshLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 26 ->
                    FallingMultiblockLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 27 ->
                    FallingBlockBatchLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 28 ->
                    MarkerLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 29 ->
                    PlayerInformLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 30 ->
                    OreDensityLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 31 ->
                    PlayerPropsLayout.receiveFields(
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
