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

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluidPropertySyncCodec;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.Map;
import net.minecraft.world.level.material.Fluid;

public final class FluidPropertySyncLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 0;
    public static final int MIN_BODY_BYTES = 4;
    public static final int MAX_BODY_BYTES = 1048576;

    private FluidPropertySyncLayout() {}

    public static EncodedPayload encode(Map<Fluid, NTMFluidProperty> properties) {
        EncodedPayload payload =
                EncodedPayload.allocateManaged(
                        PacketFamilyPrejoinFluidProperties.TYPE, 4, MAX_BODY_BYTES, true, false);
        try {
            ByteBuf encodedBuffer = payload.buffer();
            int bufferStart = encodedBuffer.writerIndex();
            ByteBuf codecBuffer = encodedBuffer;
            NTMFluidPropertySyncCodec.STREAM_CODEC.encode(codecBuffer, properties);
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
        Map<Fluid, NTMFluidProperty> properties =
                NTMFluidPropertySyncCodec.STREAM_CODEC.decode(codecBuffer);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        NTMFluidProperties.acceptSync(properties);
    }
}
