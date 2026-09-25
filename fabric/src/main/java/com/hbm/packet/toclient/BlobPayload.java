// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.ClientSyncRecovery;
import com.hbm.lib.Library;
import com.hbm.packet.BlobSynced;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.SyncWire;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.network.codec.StreamCodec;

public final class BlobPayload extends ThreadedPayload {
    public static final Type<BlobPayload> TYPE = new Type<>(Library.id("blob"));
    public static final StreamCodec<ByteBuf, BlobPayload> STREAM_CODEC =
            streamCodec(BlobPayload::decode);
    private final BlockPos pos;
    private final int type;
    private final long revision, base;
    private byte[] bytes;
    private long[] changed;
    private int first, last;
    private int bodyStart;
    private ByteBuf received;

    private BlobPayload(BlockPos pos, int type, long revision, long base) {
        this.pos = pos;
        this.type = type;
        this.revision = revision;
        this.base = base;
    }

    public static BlobPayload snapshot(
            BlockPos pos,
            int type,
            long revision,
            long base,
            byte[] bytes,
            long[] changed,
            int first,
            int last) {
        BlobPayload payload = new BlobPayload(pos, type, revision, base);
        payload.bytes = bytes;
        payload.changed = changed;
        payload.first = first;
        payload.last = last;
        payload.getCompiledBuffer();
        payload.bytes = null;
        payload.changed = null;
        return payload;
    }

    private static BlobPayload decode(ByteBuf input) {
        BlobPayload payload =
                new BlobPayload(
                        BlockPos.of(input.readLong()),
                        VarInt.read(input),
                        VarLong.read(input),
                        VarLong.read(input));
        if (payload.revision <= 0 || payload.base < 0 || payload.base >= payload.revision)
            throw new DecoderException("Invalid blob revision");
        payload.received = input.readRetainedSlice(input.readableBytes());
        return payload;
    }

    public long revision() {
        return revision;
    }

    public BlockPos pos() {
        return pos;
    }

    public int blockEntityType() {
        return type;
    }

    public CompoundTag recordedTag() {
        ByteBuf body = Unpooled.buffer();
        body.writeBoolean(base == 0);
        ByteBuf frame = received != null ? received : getCompiledBuffer();
        int start = received != null ? received.readerIndex() : bodyStart;
        body.writeBytes(frame, start, frame.writerIndex() - start);
        CompoundTag tag = new CompoundTag();
        tag.putByteArray(SyncWire.BLOB_KEY, ByteBufUtil.getBytes(body));
        return tag;
    }

    public long base() {
        return base;
    }

    public boolean smallerThanFull(int length) {
        return getCompiledBuffer().writerIndex() - bodyStart + VarLong.getByteSize(base) - 1
                < length;
    }

    public static void handle(BlobPayload payload, IPayloadHandlerContext context) {
        try {
            var player = context.playerOrNull();
            if (player == null) return;
            var level = player.level();
            var be = ChunkUtil.blockEntityIfLoaded(level, payload.pos);
            if (be instanceof BlobSynced source
                    && BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(be.getType()) == payload.type
                    && source.syncBlob().apply(payload.revision, payload.base, payload.received)) {
                ClientSyncRecovery.received(payload.pos, 2);
            } else ClientSyncRecovery.request(level, payload.pos, payload.type, 2);
        } finally {
            payload.release();
        }
    }

    @Override
    protected int bodyCapacity() {
        return 32 + (base == 0 ? bytes.length : Math.min(bytes.length, 256));
    }

    @Override
    public void toBytes(ByteBuf output) {
        output.writeLong(pos.asLong());
        VarInt.write(output, type);
        VarLong.write(output, revision);
        VarLong.write(output, base);
        bodyStart = output.writerIndex();
        if (base == 0) output.writeBytes(bytes);
        else {
            int cursor = first << 6;
            int limit = Math.min(bytes.length, (last + 1) << 6);
            while (cursor < limit) {
                int start = nextChanged(cursor, limit);
                if (start == limit) break;
                int end = start + 1;
                while (end < limit && (changed[end >>> 6] & (1L << end)) != 0) end++;
                VarInt.write(output, start);
                VarInt.write(output, end - start);
                output.writeBytes(bytes, start, end - start);
                cursor = end;
            }
        }
    }

    private int nextChanged(int start, int limit) {
        int word = start >>> 6;
        long bits = changed[word] & (-1L << start);
        while (bits == 0) {
            if (++word > last) return limit;
            bits = changed[word];
        }
        return Math.min(limit, (word << 6) + Long.numberOfTrailingZeros(bits));
    }

    public static void apply(byte[] bytes, long base, ByteBuf input) {
        if (base == 0) {
            if (input.readableBytes() != bytes.length)
                throw new DecoderException("Invalid blob length");
            input.readBytes(bytes);
        } else {
            int cursor = 0;
            while (input.isReadable()) {
                int start = VarInt.read(input), count = VarInt.read(input);
                if (start < cursor
                        || start > bytes.length
                        || count <= 0
                        || count > bytes.length - start
                        || count > input.readableBytes()) {
                    throw new DecoderException("Invalid blob range");
                }
                input.readBytes(bytes, start, count);
                cursor = start + count;
            }
        }
    }

    @Override
    protected void releaseBody() {
        if (received != null) received.release();
    }

    @Override
    public Type<BlobPayload> type() {
        return TYPE;
    }
}
