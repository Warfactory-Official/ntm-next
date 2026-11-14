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

package com.hbm.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

import static com.hbm.lib.internal.UnsafeHolder.U;
import static com.hbm.lib.internal.UnsafeHolder.fieldOffset;

public final class ChunkRequests implements AutoCloseable {
    private static final long OFF_PUMP_QUEUED = fieldOffset(ChunkRequests.class, "pumpQueued");
    private final ServerLevel level;
    private final LongConsumer ready;
    private final ConcurrentLinkedQueue<Request> incoming = new ConcurrentLinkedQueue<>();
    private final Long2ObjectOpenHashMap<Request> pending = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<Request> awaitingFlush =
            new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Request> inFlight = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<Request> completed =
            new Long2ObjectLinkedOpenHashMap<>();
    private final LongOpenHashSet held = new LongOpenHashSet();
    private volatile boolean closed;
    private int retained;
    private final @Nullable Consumer<Throwable> failureHandler;
    private volatile long admissionDeadline;
    private int admissionLimit;
    private volatile int pumpQueued;

    public ChunkRequests(ServerLevel level, LongConsumer ready) {
        this(level, ready, null);
    }

    public ChunkRequests(
            ServerLevel level, LongConsumer ready, @Nullable Consumer<Throwable> failureHandler) {
        this.level = level;
        this.ready = ready;
        this.failureHandler = failureHandler;
    }

    public synchronized void request(long chunkPos) {
        if (closed || pending.containsKey(chunkPos)) return;
        var request = new Request(chunkPos);
        if (!held.contains(chunkPos)) retained++;
        pending.put(chunkPos, request);
        incoming.offer(request);
        queuePump();
    }

    public boolean pump(long deadline, int maxInFlight) {
        if (closed) return false;
        admissionLimit = maxInFlight;
        admissionDeadline = deadline;
        boolean progressed = deliver(deadline);
        progressed |= harvest(deadline);
        progressed |= deliver(deadline);
        while (!closed
                && System.nanoTime() < deadline
                && awaitingFlush.size() + inFlight.size() + completed.size() < maxInFlight) {
            Request request = incoming.poll();
            if (request == null) break;
            long chunkPos = request.chunkPos;
            boolean added;
            synchronized (this) {
                if (pending.get(chunkPos) != request) continue;
                added = held.add(chunkPos);
            }
            if (added) ChunkUtil.addBlastTicket(level, chunkPos);
            awaitingFlush.put(chunkPos, request);
            progressed = true;
        }
        if (!closed && !awaitingFlush.isEmpty() && System.nanoTime() < deadline) {
            ChunkUtil.flushChunkTickets(level);
            if (closed) return progressed;
            var iterator = awaitingFlush.long2ObjectEntrySet().fastIterator();
            while (iterator.hasNext() && System.nanoTime() < deadline) {
                var entry = iterator.next();
                Request request = entry.getValue();
                var future = ChunkUtil.blastChunkFuture(level, entry.getLongKey());
                if (future == null) continue;
                progressed |= request.future != future;
                request.future = future;
                inFlight.put(entry.getLongKey(), request);
                if (failureHandler != null && !future.isDone())
                    future.whenComplete(
                            (_, _) -> queuePump());
                iterator.remove();
            }
        }
        progressed |= harvest(deadline);
        return deliver(deadline) | progressed;
    }

    private boolean harvest(long deadline) {
        if (closed) return false;
        boolean progressed = false;
        var iterator = inFlight.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext() && System.nanoTime() < deadline) {
            var entry = iterator.next();
            Request request = entry.getValue();
            var future = request.future;
            if (!future.isDone()) continue;
            long chunkPos = entry.getLongKey();
            iterator.remove();
            if (future.join().orElse(null) == null
                    || ChunkUtil.liveChunkNow(level, chunkPos) == null) {
                awaitingFlush.put(chunkPos, request);
            } else {
                completed.put(chunkPos, request);
                progressed = true;
            }
        }
        return progressed;
    }

    private boolean deliver(long deadline) {
        boolean progressed = false;
        while (!closed && !completed.isEmpty() && System.nanoTime() < deadline) {
            long chunkPos = completed.firstLongKey();
            Request request = completed.remove(chunkPos);
            if (ChunkUtil.liveChunkNow(level, chunkPos) == null) {
                awaitingFlush.put(chunkPos, request);
                continue;
            }
            synchronized (this) {
                pending.remove(chunkPos);
            }
            ready.accept(chunkPos);
            progressed = true;
        }
        return progressed;
    }

    private void queuePump() {
        if (failureHandler == null || closed || System.nanoTime() >= admissionDeadline) return;
        if (!U.compareAndSetInt(this, OFF_PUMP_QUEUED, 0, 1)) return;
        var server = level.getServer();
        server.schedule(server.wrapRunnable(this::pumpQueued));
    }

    private void pumpQueued() {
        U.putIntRelease(this, OFF_PUMP_QUEUED, 0);
        long deadline = admissionDeadline;
        if (closed || System.nanoTime() >= deadline) return;
        try {
            pump(deadline, admissionLimit);
        } catch (Throwable failure) {
            failureHandler.accept(failure);
        }
    }

    public synchronized int retainedCount() {
        return retained;
    }

    public void release(long chunkPos) {
        boolean releaseClaim;
        synchronized (this) {
            Request request = pending.remove(chunkPos);
            releaseClaim = held.remove(chunkPos);
            if (request != null || releaseClaim) retained--;
        }
        awaitingFlush.remove(chunkPos);
        inFlight.remove(chunkPos);
        completed.remove(chunkPos);
        if (releaseClaim) ChunkUtil.releaseChunkAsync(level, chunkPos);
    }

    public synchronized boolean isPending(long chunkPos) {
        return pending.containsKey(chunkPos);
    }

    public boolean holds(long chunkPos) {
        return held.contains(chunkPos);
    }

    public synchronized int pendingCount() {
        return pending.size();
    }

    public int heldCount() {
        return held.size();
    }

    public void releaseAll() {
        synchronized (this) {
            pending.clear();
            incoming.clear();
            retained = 0;
        }
        awaitingFlush.clear();
        inFlight.clear();
        completed.clear();
        for (long chunkPos : held) ChunkUtil.releaseChunkAsync(level, chunkPos);
        synchronized (this) {
            held.clear();
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
        }
        releaseAll();
    }

    private static final class Request {
        private final long chunkPos;
        private @Nullable CompletableFuture<ChunkResult<LevelChunk>> future;

        private Request(long chunkPos) {
            this.chunkPos = chunkPos;
        }
    }
}
