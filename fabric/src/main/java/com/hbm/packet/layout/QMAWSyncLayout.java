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
import com.hbm.packet.codec.PacketBuffers;
import com.hbm.packet.sync.SyncPacketHandlers;
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.qmaw.QMAWSyncCodecs;
import com.hbm.qmaw.QuickManualAndWiki;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class QMAWSyncLayout {
    public static final int VARIANT = 0;
    public static final int VARIANT_BITS = 0;
    public static final int MIN_BODY_BYTES = 1;
    public static final int MAX_BODY_BYTES = 67108864;

    private QMAWSyncLayout() {}

    public static EncodedPayload encode(Map<Identifier, QuickManualAndWiki> pages) {
        EncodedPayload payload =
                EncodedPayload.allocateManaged(
                        PacketFamilyDatapackQmaw.TYPE, 1, MAX_BODY_BYTES, true, true);
        try {
            ByteBuf encodedBuffer = payload.buffer();
            int bufferStart = encodedBuffer.writerIndex();
            var codecBuffer = PacketBuffers.registry(encodedBuffer, true);
            QMAWSyncCodecs.PAGES.encode(codecBuffer, pages);
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
        var codecBuffer = PacketBuffers.registry(decodedBuffer, context);
        Map<Identifier, QuickManualAndWiki> pages = QMAWSyncCodecs.PAGES.decode(codecBuffer);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        SyncPacketHandlers.publishQmaw(pages);
    }
}
