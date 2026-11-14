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

package com.hbm.packet;

import com.hbm.packet.layout.BulkDownloadLayout;
import com.hbm.packet.layout.BulkUploadLayout;
import com.hbm.packet.layout.PacketFamilyBulkCopy;
import com.hbm.packet.layout.PacketFamilyBulkPaste;
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.world.bulk.BulkCopyRequests;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.InflaterInputStream;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class BulkTransfers {
    private static final int LIMIT = 16 * 1024;
    private static final Map<Player, Long2ObjectOpenHashMap<Pending>> UPLOADS = new WeakHashMap<>();
    private static final Map<Player, Long2ObjectOpenHashMap<Pending>> DOWNLOADS =
            new WeakHashMap<>();
    private static long nextUpload;
    private static long nextDownload;

    private BulkTransfers() {}

    public static void disconnected(Player player) {
        if (player instanceof ServerPlayer) UPLOADS.remove(player);
        else {
            DOWNLOADS.remove(player);
            BulkCopyRequests.disconnected();
        }
    }

    public static void sendPaste(EncodedPayload payload) {
        write(payload, true, PacketWire::sendToServer);
    }

    public static void sendCopy(EncodedPayload payload, ServerPlayer player) {
        write(payload, false, part -> PacketWire.sendTo(part, player));
    }

    static void write(EncodedPayload payload, boolean upload, Consumer<EncodedPayload> send) {
        ByteBuf frame = payload.buffer();
        int start = payload.bodyReaderIndex();
        if (frame.writerIndex() - start <= LIMIT) {
            send.accept(payload);
            return;
        }
        byte[] body = new byte[frame.writerIndex() - start];
        try {
            frame.getBytes(start, body);
        } finally {
            payload.release();
        }
        byte[] packed = new byte[body.length];
        int packedLength = deflate(body, packed);
        boolean compressed = packedLength >= 0;
        byte[] data = compressed ? packed : body;
        int total = compressed ? packedLength : body.length;
        long message = upload ? ++nextUpload : ++nextDownload;
        for (int offset = 0, index = 0; offset < total; offset += LIMIT, index++) {
            send.accept(
                    upload
                            ? BulkUploadLayout.encode(
                                    message, total, body.length, index, compressed, data, offset)
                            : BulkDownloadLayout.encode(
                                    message, total, body.length, index, compressed, data, offset));
        }
    }

    private static int deflate(byte[] body, byte[] packed) {
        Deflater compressor = new Deflater(Deflater.BEST_SPEED);
        try {
            compressor.setInput(body);
            compressor.finish();
            int length = 0;
            while (!compressor.finished()) {
                if (length == packed.length) return -1;
                length += compressor.deflate(packed, length, packed.length - length);
            }
            return length < body.length ? length : -1;
        } finally {
            compressor.end();
        }
    }

    public static void upload(
            PayloadHandlerContext context,
            long message,
            int total,
            int rawLength,
            int index,
            boolean compressed,
            byte[] data) {
        check(total, rawLength, index, data);
        if (!(context.player() instanceof ServerPlayer player)
                || !Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)
                        .test(player.createCommandSourceStack())) return;
        ByteBuf body = receive(UPLOADS, player, message, total, rawLength, index, compressed, data);
        if (body == null) return;
        try {
            if (rawLength < PacketFamilyBulkPaste.MIN_BODY_BYTES
                    || rawLength > PacketFamilyBulkPaste.MAX_BODY_BYTES) {
                throw new DecoderException("Invalid bulk paste length");
            }
            PacketFamilyBulkPaste.receive(body, context);
        } finally {
            body.release();
        }
    }

    public static void download(
            PayloadHandlerContext context,
            long message,
            int total,
            int rawLength,
            int index,
            boolean compressed,
            byte[] data) {
        check(total, rawLength, index, data);
        Player player = context.playerOrNull();
        if (player == null) return;
        ByteBuf body =
                receive(DOWNLOADS, player, message, total, rawLength, index, compressed, data);
        if (body == null) return;
        try {
            if (rawLength < PacketFamilyBulkCopy.MIN_BODY_BYTES
                    || rawLength > PacketFamilyBulkCopy.MAX_BODY_BYTES) {
                throw new DecoderException("Invalid bulk copy length");
            }
            PacketFamilyBulkCopy.receive(body, context);
        } finally {
            body.release();
        }
    }

    private static void check(int total, int rawLength, int index, byte[] data) {
        if (total <= 0
                || rawLength <= 0
                || index < 0
                || (long) index * LIMIT >= total
                || data.length != Math.min(LIMIT, total - index * LIMIT)) {
            throw new DecoderException("Malformed bulk fragment");
        }
    }

    private static @Nullable ByteBuf receive(
            Map<Player, Long2ObjectOpenHashMap<Pending>> transfers,
            Player player,
            long message,
            int total,
            int rawLength,
            int index,
            boolean compressed,
            byte[] data) {
        var messages = transfers.computeIfAbsent(player, ignored -> new Long2ObjectOpenHashMap<>());
        Pending pending =
                messages.computeIfAbsent(
                        message, ignored -> new Pending(total, rawLength, compressed));
        if (pending.total != total
                || pending.rawLength != rawLength
                || pending.compressed != compressed) {
            throw new DecoderException("Inconsistent bulk fragments");
        }
        if (pending.parts.putIfAbsent(index, data) == null) pending.received += data.length;
        if (pending.received != pending.total) return null;
        messages.remove(message);
        if (messages.isEmpty()) transfers.remove(player);
        return assemble(pending);
    }

    private static ByteBuf assemble(Pending pending) {
        byte[] body = new byte[pending.rawLength];
        int count = (int) (((long) pending.total + LIMIT - 1) / LIMIT);
        if (!pending.compressed) {
            if (pending.total != pending.rawLength)
                throw new DecoderException("Invalid bulk data length");
            for (int i = 0; i < count; i++) {
                byte[] part = pending.parts.get(i);
                System.arraycopy(part, 0, body, i * LIMIT, part.length);
            }
            return Unpooled.wrappedBuffer(body);
        }
        var wire = Unpooled.compositeBuffer(Integer.MAX_VALUE);
        for (int i = 0; i < count; i++)
            wire.addComponent(true, Unpooled.wrappedBuffer(pending.parts.get(i)));
        try (var input = new InflaterInputStream(new ByteBufInputStream(wire, true))) {
            if (input.readNBytes(body, 0, body.length) != body.length)
                throw new DecoderException("Truncated bulk data");
            if (input.read() != -1)
                throw new DecoderException("Bulk data exceeded its declared length");
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return Unpooled.wrappedBuffer(body);
    }

    private static final class Pending {
        final int total;
        final int rawLength;
        final boolean compressed;
        final Int2ObjectOpenHashMap<byte[]> parts = new Int2ObjectOpenHashMap<>();
        int received;

        Pending(int total, int rawLength, boolean compressed) {
            this.total = total;
            this.rawLength = rawLength;
            this.compressed = compressed;
        }
    }
}
