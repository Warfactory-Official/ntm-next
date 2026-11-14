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

import com.hbm.packet.layout.BlobSyncLayout;
import com.hbm.packet.threading.EncodedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SyncBlob {
    private final byte[] bytes;
    private final SyncSource owner;
    private final long position;
    private final int type;
    private final long[] changed;
    private int first = Integer.MAX_VALUE, last = -1, count;
    private long revision, previous;
    private boolean dirty, pending, enabled;
    private boolean watched = true;
    private Prepared prepared;

    public SyncBlob(byte[] bytes, BlockEntity owner) {
        this.bytes = bytes;
        this.owner = (SyncSource) owner;
        position = owner.getBlockPos().asLong();
        type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(owner.getType());
        changed = new long[(bytes.length + 63) >>> 6];
    }

    public void changed(int index) {
        if (!dirty) {
            previous = revision;
            dirty = true;
            pending = true;
            owner.syncChanged(2);
        }
        int word = index >>> 6;
        long bit = 1L << index;
        if ((changed[word] & bit) == 0) count++;
        changed[word] |= bit;
        first = Math.min(first, word);
        last = Math.max(last, word);
    }

    public boolean clear() {
        boolean modified = false;
        for (int i = 0; i < bytes.length; i++)
            if (bytes[i] != 0) {
                bytes[i] = 0;
                changed(i);
                modified = true;
            }
        return modified;
    }

    public void clearReceived() {
        Arrays.fill(bytes, (byte) 0);
        replacePrepared(null);
    }

    public void copy(byte[] values) {
        if (values.length != bytes.length)
            throw new IllegalArgumentException("Blob length changed");
        for (int i = 0; i < bytes.length; i++)
            if (bytes[i] != values[i]) {
                bytes[i] = values[i];
                changed(i);
            }
    }

    private Prepared prepare() {
        if (!dirty) {
            if (revision == 0) revision = SyncWire.nextRevision();
            if (prepared == null) prepared = new Prepared(owner, bytes.clone(), null, 0, -1, -1);
            return prepared;
        }

        revision = SyncWire.nextRevision();
        byte[] snapshot = bytes.clone();
        long[] delta = null;
        int deltaBytes = -1;
        int deltaFirst = first == Integer.MAX_VALUE ? 0 : first;
        int deltaLast = last;
        if (previous != 0 && count < bytes.length) {
            delta = Arrays.copyOf(changed, last + 1);
            deltaBytes = deltaBodyBytes(snapshot.length, delta, deltaFirst, deltaLast);
            if (deltaBytes + VarLong.getByteSize(previous) - 1 >= snapshot.length) {
                delta = null;
                deltaBytes = -1;
            }
        }
        replacePrepared(new Prepared(owner, snapshot, delta, deltaFirst, deltaLast, deltaBytes));
        if (last >= first) Arrays.fill(changed, first, last + 1, 0L);
        first = Integer.MAX_VALUE;
        last = -1;
        count = 0;
        dirty = false;
        return prepared;
    }

    public long revision() {
        return revision;
    }

    public void watching(boolean watched) {
        this.watched = watched;
    }

    public void writeInitial(ByteBuf output) {
        Level level = ((BlockEntity) owner).getLevel();
        if (!SyncWire.replayCompat() || level == null || !level.isClientSide()) prepare();
        VarLong.write(output, revision);
        output.writeBytes(bytes);
    }

    public void readInitial(ByteBuf input) {
        replacePrepared(null);
        Arrays.fill(changed, 0L);
        first = Integer.MAX_VALUE;
        last = -1;
        count = 0;
        dirty = pending = false;
        revision = VarLong.read(input);
        input.readBytes(bytes);
    }

    public void flush(BlockEntity owner, int range, boolean enabled) {
        if (enabled && !this.enabled) pending = true;
        this.enabled = enabled;
        if (!enabled || !pending || !watched || !(owner.getLevel() instanceof ServerLevel level))
            return;
        if (!PacketWire.hasPlayersTracking(level, owner.getBlockPos())) {
            watched = false;
            return;
        }
        BeSyncTable.refreshBlob(this, owner, range);
        pending = false;
    }

    void enqueue(ServerLevel level, ServerPlayer player) {
        if (!enabled) return;
        Prepared publication = prepare();
        if (ChunkTrackerIndex.blobRevision(player, position) == revision) return;
        PacketWire.enqueueBlobSync(player, level, position, type, revision, previous, publication);
    }

    public void sendFull(ServerPlayer player) {
        if (!enabled) return;
        Prepared publication = prepare();
        PacketWire.sendTo(publication.retainFrame(position, type, revision, 0L), player);
        ChunkTrackerIndex.blobDispatched(player, position, revision);
    }

    public void ingest(ByteBuf input) {
        boolean full = input.readBoolean();
        byte[] next = bytes.clone();
        applyBody(next, full ? 0 : 1, input);
        copy(next);
    }

    public boolean apply(long revision, long base, ByteBuf input) {
        if (revision <= this.revision) {
            input.skipBytes(input.readableBytes());
            return true;
        }
        if (base != 0 && base != this.revision) {
            input.skipBytes(input.readableBytes());
            return false;
        }
        applyBody(bytes, base, input);
        this.revision = revision;
        replacePrepared(null);
        return true;
    }

    private static int deltaBodyBytes(int byteLength, long[] changed, int first, int last) {
        int cursor = first << 6;
        int limit = Math.min(byteLength, (last + 1) << 6);
        int size = 0;
        while (cursor < limit) {
            int start = nextChanged(changed, last, cursor, limit);
            if (start == limit) break;
            int end = start + 1;
            while (end < limit && (changed[end >>> 6] & (1L << end)) != 0) end++;
            size = Math.addExact(size, VarInt.getByteSize(start));
            size = Math.addExact(size, VarInt.getByteSize(end - start));
            size = Math.addExact(size, end - start);
            cursor = end;
        }
        return size;
    }

    private static int nextChanged(long[] changed, int last, int start, int limit) {
        int word = start >>> 6;
        long bits = changed[word] & (-1L << start);
        while (bits == 0) {
            if (++word > last) return limit;
            bits = changed[word];
        }
        return Math.min(limit, (word << 6) + Long.numberOfTrailingZeros(bits));
    }

    private static void applyBody(byte[] bytes, long base, ByteBuf input) {
        if (base == 0) {
            if (input.readableBytes() != bytes.length)
                throw new DecoderException("Invalid blob length");
            input.readBytes(bytes);
            return;
        }

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

    public void release() {
        replacePrepared(null);
        Arrays.fill(changed, 0L);
        first = Integer.MAX_VALUE;
        last = -1;
        count = 0;
        previous = revision = 0;
        dirty = false;
        pending = true;
        enabled = false;
        watched = true;
    }

    public static final class Prepared {
        private final Object owner;
        private final byte[] bytes;
        private final long[] changed;
        private final int first, last, deltaBytes;
        private Long2ObjectOpenHashMap<EncodedPayload> frames;
        private long framePosition;
        private long frameRevision;
        private int frameBlockEntityType;
        private boolean frameIdentity;
        private int references = 1;

        private Prepared(
                Object owner, byte[] bytes, long[] changed, int first, int last, int deltaBytes) {
            this.owner = owner;
            this.bytes = bytes;
            this.changed = changed;
            this.first = first;
            this.last = last;
            this.deltaBytes = deltaBytes;
        }

        public int bodySize(long base) {
            if (base == 0) return bytes.length;
            if (changed == null) throw new IllegalArgumentException("Prepared blob has no delta");
            return deltaBytes;
        }

        public void writeBody(long base, ByteBuf output) {
            if (base == 0) {
                output.writeBytes(bytes);
                return;
            }
            if (changed == null) throw new IllegalArgumentException("Prepared blob has no delta");
            int cursor = first << 6;
            int limit = Math.min(bytes.length, (last + 1) << 6);
            while (cursor < limit) {
                int start = nextChanged(changed, last, cursor, limit);
                if (start == limit) break;
                int end = start + 1;
                while (end < limit && (changed[end >>> 6] & (1L << end)) != 0) end++;
                VarInt.write(output, start);
                VarInt.write(output, end - start);
                output.writeBytes(bytes, start, end - start);
                cursor = end;
            }
        }

        public boolean hasDelta() {
            return changed != null;
        }

        public boolean ownedBy(Object owner) {
            return this.owner == owner;
        }

        public synchronized EncodedPayload retainFrame(
                long position, int blockEntityType, long revision, long base) {
            if (references == 0) throw new IllegalStateException("Released blob publication");
            if (PacketWire.ccpPacketIdBytes() == null) {
                return BlobSyncLayout.encode(
                        BlockPos.of(position), blockEntityType, revision, base, this);
            }
            if (!frameIdentity) {
                frameIdentity = true;
                framePosition = position;
                frameBlockEntityType = blockEntityType;
                frameRevision = revision;
            } else if (framePosition != position
                    || frameBlockEntityType != blockEntityType
                    || frameRevision != revision) {
                throw new IllegalArgumentException(
                        "Blob publication reused for another block entity revision");
            }
            EncodedPayload frame = frames == null ? null : frames.get(base);
            if (frame == null) {
                frame =
                        BlobSyncLayout.encode(
                                BlockPos.of(position), blockEntityType, revision, base, this);
                if (frames == null) frames = new Long2ObjectOpenHashMap<>(2);
                frames.put(base, frame);
            }
            frame.retain();
            return frame;
        }

        public synchronized void retain() {
            if (references == 0 || references == Integer.MAX_VALUE) {
                throw new IllegalStateException("Invalid blob publication retain");
            }
            references++;
        }

        public synchronized void release() {
            if (references == 0)
                throw new IllegalStateException("Blob publication already released");
            if (--references != 0 || frames == null) return;
            for (EncodedPayload frame : frames.values()) frame.release();
            frames.clear();
            frames = null;
        }
    }

    private void replacePrepared(Prepared next) {
        Prepared previous = prepared;
        prepared = next;
        if (previous != null) previous.release();
    }
}
