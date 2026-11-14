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

import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.Recycler;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

final class OutboundBatch implements Runnable {
    private static final Recycler<OutboundBatch> POOL =
            new Recycler<>(256, false) {
                @Override
                protected OutboundBatch newObject(Handle<OutboundBatch> handle) {
                    return new OutboundBatch(handle);
                }
            };
    private final Recycler.Handle<OutboundBatch> handle;
    private final Long2IntOpenHashMap syncSlots = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap blobSlots = new Long2IntOpenHashMap();
    private ThreadedPayload[] frames = new ThreadedPayload[16];
    private SyncUnitState.Snapshot[] units = new SyncUnitState.Snapshot[16];
    private SyncBlob.Prepared[] blobs = new SyncBlob.Prepared[16];
    private long[] positions = new long[16];
    private long[] revisions = new long[16];
    private long[] bases = new long[16];
    private int[] blockEntityTypes = new int[16];
    private byte[] state = new byte[16];
    private int size;
    private ServerLevel syncLevel;
    private Channel channel;
    private ChannelHandlerContext framing;
    private int threshold;
    private Connection connection;
    ServerPlayer player;
    OutboundBatch next;

    private OutboundBatch(Recycler.Handle<OutboundBatch> handle) {
        this.handle = handle;
    }

    static OutboundBatch take(ServerPlayer player) {
        Channel channel = player.connection.connection.channel;
        ChannelHandlerContext framing = PacketWire.framingContext(channel);
        ChannelHandlerContext compression = channel.pipeline().context("compress");
        OutboundBatch batch = POOL.get();
        batch.player = player;
        batch.channel = channel;
        batch.connection =
                PacketWire.managedTransport() && !(channel instanceof EmbeddedChannel)
                        ? player.connection.connection
                        : null;
        batch.framing = framing;
        batch.threshold =
                framing == null || compression == null
                        ? -1
                        : ((CompressionEncoder) compression.handler()).getThreshold();
        return batch;
    }

    private void ensureCapacity() {
        if (size == frames.length) {
            int capacity = size << 1;
            frames = Arrays.copyOf(frames, capacity);
            units = Arrays.copyOf(units, capacity);
            blobs = Arrays.copyOf(blobs, capacity);
            positions = Arrays.copyOf(positions, capacity);
            revisions = Arrays.copyOf(revisions, capacity);
            bases = Arrays.copyOf(bases, capacity);
            blockEntityTypes = Arrays.copyOf(blockEntityTypes, capacity);
            state = Arrays.copyOf(state, capacity);
        }
    }

    private int append(ThreadedPayload payload) {
        ensureCapacity();
        payload.retain();
        frames[size] = payload;
        return size++;
    }

    void add(ThreadedPayload payload) {
        int index = append(payload);
        state[index] = 0;
    }

    void addUnitSync(
            ServerLevel level,
            long position,
            int blockEntityType,
            SyncUnitState.Snapshot publication) {
        if (syncLevel != null && syncLevel != level) {
            for (int i = 0; i < size; i++) if (state[i] != 0) release(i);
            syncSlots.clear();
            blobSlots.clear();
        }
        syncLevel = level;
        boolean event = publication.event();
        int previous = 0;
        if (event) syncSlots.remove(position);
        else previous = syncSlots.get(position);
        if (previous != 0) {
            int index = previous - 1;
            if (units[index] == publication) return;
            if (state[index] == 1 && revisions[index] >= publication.revision()) return;
            release(index);
            publication.retain();
            units[index] = publication;
            positions[index] = position;
            blockEntityTypes[index] = blockEntityType;
            revisions[index] = publication.revision();
            state[index] = 1;
            return;
        }
        ensureCapacity();
        int index = size++;
        publication.retain();
        units[index] = publication;
        positions[index] = position;
        blockEntityTypes[index] = blockEntityType;
        revisions[index] = publication.revision();
        state[index] = 1;
        if (!event) syncSlots.put(position, index + 1);
    }

    void addBlobSync(
            ServerLevel level,
            long position,
            int blockEntityType,
            long revision,
            long previousRevision,
            SyncBlob.Prepared publication) {
        if (syncLevel != null && syncLevel != level) {
            for (int i = 0; i < size; i++) if (state[i] != 0) release(i);
            syncSlots.clear();
            blobSlots.clear();
        }
        syncLevel = level;
        int previous = blobSlots.get(position);
        if (previous != 0) {
            int index = previous - 1;
            if (blobs[index] == publication && revisions[index] == revision) return;
            if (state[index] == 2 && revisions[index] >= revision) return;
            release(index);
            publication.retain();
            blobs[index] = publication;
            positions[index] = position;
            blockEntityTypes[index] = blockEntityType;
            revisions[index] = revision;
            bases[index] = previousRevision;
            state[index] = 2;
            return;
        }
        ensureCapacity();
        int index = size++;
        publication.retain();
        blobs[index] = publication;
        positions[index] = position;
        blockEntityTypes[index] = blockEntityType;
        revisions[index] = revision;
        bases[index] = previousRevision;
        state[index] = 2;
        blobSlots.put(position, index + 1);
    }

    void dropPosition(ServerLevel level, long position, BlockEntity owner) {
        if (syncLevel != level) return;
        for (int i = 0; i < size; i++) {
            if (state[i] != 1 || positions[i] != position || !units[i].ownedBy(owner)) continue;
            if (syncSlots.get(position) == i + 1) syncSlots.remove(position);
            release(i);
        }
        int slot = blobSlots.get(position);
        if (slot != 0 && blobs[slot - 1].ownedBy(owner)) {
            blobSlots.remove(position);
            release(slot - 1);
        }
    }

    void dropChunk(ServerLevel level, long chunkKey) {
        if (syncLevel != level) return;
        for (int i = 0; i < size; i++) {
            if (state[i] == 0) continue;
            long position = positions[i];
            if (ChunkPos.pack(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4)
                    == chunkKey) {
                (state[i] == 2 ? blobSlots : syncSlots).remove(position);
                release(i);
            }
        }
    }

    Channel channel() {
        return channel;
    }

    void dispatch() {
        dispatch(false);
    }

    void dispatch(boolean immediate) {
        for (int i = 0; i < size; i++) {
            if (state[i] == 2) {
                long current = ChunkTrackerIndex.blobRevision(player, positions[i]);
                if (current >= revisions[i]) {
                    release(i);
                    continue;
                }
                long base = blobs[i].hasDelta() && current == bases[i] ? bases[i] : 0;
                SyncBlob.Prepared publication = blobs[i];
                frames[i] =
                        publication.retainFrame(
                                positions[i], blockEntityTypes[i], revisions[i], base);
                blobs[i] = null;
                state[i] = 0;
                publication.release();
                ChunkTrackerIndex.blobDispatched(player, positions[i], revisions[i]);
                continue;
            }
            if (state[i] != 1) continue;
            SyncUnitState.Snapshot publication = units[i];
            long base = ChunkTrackerIndex.revision(player, positions[i]);
            long revision = revisions[i];
            if (Math.abs(base) >= revision) {
                release(i);
                continue;
            }
            long selectedBase = publication.base(base);
            long selectedUnits = publication.selectedUnits(selectedBase);
            frames[i] =
                    publication.retainFrame(
                            positions[i], blockEntityTypes[i], selectedBase, selectedUnits);
            units[i] = null;
            state[i] = 0;
            publication.release();
            ChunkTrackerIndex.dispatched(player, positions[i], revision);
        }
        boolean pending = false;
        for (int i = 0; i < size; i++) {
            ThreadedPayload payload = frames[i];
            if (payload == null) continue;
            pending = true;
            if (connection != null) {
                final ServerPlayer player2 = player;
                final ClientboundCustomPayloadPacket packet = payload.managedPacket();
                {
                }
            }
        }
        if (!pending) {
            discard();
            return;
        }
        if (immediate && channel.eventLoop().inEventLoop()) run();
        else channel.eventLoop().execute(this);
    }

    private void sendManaged() {
        for (int i = 0; i < size; i++) {
            if (!channel.isOpen()) return;
            ThreadedPayload payload = frames[i];
            if (payload != null) connection.send(payload.managedPacket(), null, false);
        }
        if (channel.isOpen()) connection.flushChannel();
    }

    @Override
    public void run() {
        try {
            if (!channel.isOpen()) return;
            if (connection != null) {
                sendManaged();
                return;
            }
            for (int i = 0; i < size; i++) {
                ThreadedPayload payload = frames[i];
                if (payload == null) continue;
                frames[i] = null;
                try {
                    ByteBuf wire =
                            framing == null
                                    ? payload.getCompiledBuffer()
                                    : payload.getSocketBuffer(threshold);
                    if (framing == null)
                        channel.write(wire.retainedDuplicate(), channel.voidPromise());
                    else framing.write(wire.retainedDuplicate(), channel.voidPromise());
                } finally {
                    payload.release();
                }
            }
            channel.flush();
        } catch (Throwable failure) {
            channel.pipeline().fireExceptionCaught(failure);
            channel.close();
        } finally {
            discard();
        }
    }

    private void release(int index) {
        ThreadedPayload payload = frames[index];
        if (payload != null) {
            payload.release();
            frames[index] = null;
        }
        SyncUnitState.Snapshot unitPublication = units[index];
        if (unitPublication != null) unitPublication.release();
        SyncBlob.Prepared blobPublication = blobs[index];
        if (blobPublication != null) blobPublication.release();
        units[index] = null;
        blobs[index] = null;
        revisions[index] = 0;
        bases[index] = 0;
        blockEntityTypes[index] = 0;
        positions[index] = 0;
        state[index] = 0;
    }

    void discard() {
        for (int i = 0; i < size; i++) release(i);
        size = 0;
        syncSlots.clear();
        blobSlots.clear();
        syncLevel = null;
        channel = null;
        connection = null;
        framing = null;
        player = null;
        next = null;
        handle.recycle(this);
    }
}
