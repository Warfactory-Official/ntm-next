// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.packet.toclient.BlobPayload;
import com.hbm.packet.toclient.UnitPayload;
import com.hbm.platform.Services;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

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
    private long[] positions = new long[16];
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
        Channel channel = Services.NETWORK.channelOf(player);
        ChannelHandlerContext framing = PacketWire.framingContext(channel);
        ChannelHandlerContext compression = channel.pipeline().context("compress");
        OutboundBatch batch = POOL.get();
        batch.player = player;
        batch.channel = channel;

        batch.connection =
                PacketWire.managedTransport() && !(channel instanceof EmbeddedChannel)
                        ? Services.NETWORK.connectionOf(player)
                        : null;
        batch.framing = framing;
        batch.threshold =
                framing == null || compression == null
                        ? -1
                        : ((CompressionEncoder) compression.handler()).getThreshold();
        return batch;
    }

    private int append(ThreadedPayload payload) {
        if (size == frames.length) {
            int capacity = size << 1;
            frames = Arrays.copyOf(frames, capacity);
            positions = Arrays.copyOf(positions, capacity);
            state = Arrays.copyOf(state, capacity);
        }
        payload.retain();
        frames[size] = payload;
        return size++;
    }

    void add(ThreadedPayload payload) {
        int index = append(payload);
        state[index] = 0;
    }

    void addSync(ServerLevel level, long position, ThreadedPayload payload) {
        if (syncLevel != null && syncLevel != level) {
            for (int i = 0; i < size; i++) if (state[i] != 0) release(i);
            syncSlots.clear();
            blobSlots.clear();
        }
        syncLevel = level;
        var slots = payload instanceof BlobPayload ? blobSlots : syncSlots;
        int previous = slots.get(position);
        if (previous != 0) {
            int index = previous - 1;
            if (frames[index] == payload) return;
            if (frames[index] instanceof UnitPayload queued
                    && payload instanceof UnitPayload incoming
                    && queued.revision() >= incoming.revision()) return;
            payload.retain();
            frames[index].release();
            frames[index] = payload;
            return;
        }
        int index = append(payload);
        state[index] = (byte) (payload instanceof BlobPayload ? 2 : 1);
        positions[index] = position;
        slots.put(position, index + 1);
    }

    void dropPosition(ServerLevel level, long position) {
        if (syncLevel != level) return;
        int slot = syncSlots.remove(position);
        if (slot != 0) release(slot - 1);
        slot = blobSlots.remove(position);
        if (slot != 0) release(slot - 1);
    }

    void dropChunk(ServerLevel level, long chunkKey) {
        if (syncLevel != level) return;
        for (int i = 0; i < size; i++) {
            if (state[i] == 0 || frames[i] == null) continue;
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
            if (state[i] == 2 && frames[i] instanceof BlobPayload blob) {
                long base = ChunkTrackerIndex.blobRevision(player, positions[i]);
                if (base >= blob.revision()) release(i);
                else {
                    assert blob.base() == 0 || blob.base() == base;
                    ChunkTrackerIndex.blobDispatched(player, positions[i], blob.revision());
                }
                continue;
            }
            if (state[i] != 1
                    || !(frames[i] instanceof UnitPayload publication)
                    || publication.revision() == 0) continue;
            long base = ChunkTrackerIndex.revision(player, positions[i]);
            long revision = publication.revision();
            if (Math.abs(base) >= revision) {
                release(i);
                continue;
            }
            ThreadedPayload frame = publication.forBase(base);
            if (frame != frames[i]) {
                frame.retain();
                frames[i].release();
                frames[i] = frame;
            }
            ChunkTrackerIndex.dispatched(player, positions[i], revision);
        }
        boolean pending = false;
        for (int i = 0; i < size; i++) {
            ThreadedPayload payload = frames[i];
            if (payload == null) continue;
            pending = true;
            if (connection != null)
                Services.NETWORK.validateClientboundPayload(player, payload.managedPacket());
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
