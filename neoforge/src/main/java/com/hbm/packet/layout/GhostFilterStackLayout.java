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

import com.hbm.blockentity.GhostFilters;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.packet.ControlAuthority;
import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.codec.PacketBuffers;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class GhostFilterStackLayout {
    public static final int VARIANT = 128;
    public static final int VARIANT_BITS = 8;
    public static final int MIN_BODY_BYTES = 7;
    public static final int MAX_BODY_BYTES = 32767;

    private GhostFilterStackLayout() {}

    public static EncodedPayload encode(int containerId, int slot, ItemStack stack) {
        if (((long) (containerId - (1)) & 0xFFFFFFFFL) > 99L)
            throw new IllegalArgumentException("Invalid containerId");
        EncodedPayload payload =
                EncodedPayload.allocate(
                        PacketFamilyControls.TYPE, 7, MAX_BODY_BYTES, false, false, true);
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
            var codecBuffer = PacketBuffers.registry(encodedBuffer, false);
            {
                long containerIdBits = ((long) containerId - 1L);
                long slotBits = slot;
                U.putInt(
                        storageBase,
                        storageAddress,
                        (int) (128L | (containerIdBits << 8) | (slotBits << 15)));
                U.putShort(
                        storageBase, storageAddress + 4L, (short) (((slotBits >>> 17) & 0x7FFFL)));
            }
            encodedBuffer.writerIndex(bufferStart + 6);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(codecBuffer, stack);
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
        long containerIdToSlotWord = (U.getInt(storageBase, storageAddress + 1L) & 0xFFFFFFFFL);
        long slotWord = (U.getByte(storageBase, storageAddress + 5L) & 255L);
        long containerIdBits = (containerIdToSlotWord & 127L);
        if (containerIdBits > 99L) throw new DecoderException("Invalid containerId");
        int containerId = (int) (containerIdBits + 1L);
        int slot = (int) ((containerIdToSlotWord >>> 7) | (slotWord << 25));
        if ((slotWord >>> 7) != 0) throw new DecoderException("Nonzero packet padding");
        decodedBuffer.readerIndex(bufferStart + 6);
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(codecBuffer);
        if (decodedBuffer.readerIndex() != bufferStart + bodyBytes)
            throw new DecoderException("Unexpected packet tail");
        if (!(context.player() instanceof ServerPlayer player)
                || player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof BlockEntityMenu<?> menu)
                || !(menu.blockEntity() instanceof GhostFilters receiver)) return;
        receiver.putFilter(slot, stack);
    }
}
