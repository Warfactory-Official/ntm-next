// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import com.hbm.packet.toclient.BlobPayload;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.VarLong;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SyncBlob {
    private final byte[] bytes;
    private final SyncSource owner;
    private final BlockPos pos;
    private final int type;
    private final long[] changed;
    private int first = Integer.MAX_VALUE, last = -1, count;
    private long revision, previous;
    private boolean dirty, pending, enabled;
    private boolean watched = true;
    private BlobPayload full, delta;

    public SyncBlob(byte[] bytes, BlockEntity owner) {
        this.bytes = bytes;
        this.owner = (SyncSource) owner;
        pos = owner.getBlockPos();
        type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(owner.getType());
        changed = new long[(bytes.length + 63) >>> 6];
    }

    public void changed(int index) {
        if (!dirty) {
            releaseFrames();
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

    public void clear() {
        for (int i = 0; i < bytes.length; i++)
            if (bytes[i] != 0) {
                bytes[i] = 0;
                changed(i);
            }
    }

    public void clearReceived() {
        Arrays.fill(bytes, (byte) 0);
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

    private void prepare() {
        if (!dirty) {
            if (revision == 0) revision = SyncWire.nextRevision();
            return;
        }
        revision = SyncWire.nextRevision();
        if (previous != 0 && count < bytes.length) {
            delta =
                    BlobPayload.snapshot(
                            pos, type, revision, previous, bytes, changed, first, last);
            if (!delta.smallerThanFull(bytes.length)) {
                delta.release();
                delta = null;
            }
        }
        if (last >= first) Arrays.fill(changed, first, last + 1, 0L);
        first = Integer.MAX_VALUE;
        last = -1;
        count = 0;
        dirty = false;
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
        releaseFrames();
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
        prepare();
        long base = ChunkTrackerIndex.blobRevision(player, pos.asLong());
        if (base == revision) return;
        BlobPayload payload = delta != null && base == previous ? delta : full();
        PacketWire.enqueueSync(player, level, pos.asLong(), payload);
    }

    private BlobPayload full() {
        prepare();
        if (full == null) full = BlobPayload.snapshot(pos, type, revision, 0, bytes, null, 0, 0);
        return full;
    }

    public void sendFull(ServerPlayer player) {
        if (!enabled) return;
        BlobPayload payload = full();
        payload.retain();
        PacketWire.sendTo(payload, player);
        ChunkTrackerIndex.blobDispatched(player, pos.asLong(), revision);
    }

    public void ingest(ByteBuf input) {
        boolean full = input.readBoolean();
        byte[] next = bytes.clone();
        BlobPayload.apply(next, full ? 0 : 1, input);
        copy(next);
    }

    public boolean apply(long revision, long base, ByteBuf input) {
        if (revision <= this.revision) return true;
        if (base != 0 && base != this.revision) return false;
        BlobPayload.apply(bytes, base, input);
        this.revision = revision;
        return true;
    }

    private void releaseFrames() {
        if (full != null) {
            full.release();
            full = null;
        }
        if (delta != null) {
            delta.release();
            delta = null;
        }
    }

    public void release() {
        releaseFrames();
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
}
