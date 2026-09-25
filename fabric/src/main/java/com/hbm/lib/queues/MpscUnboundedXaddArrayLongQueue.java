// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: JCTools contributors
// SPDX-License-Identifier: Apache-2.0 AND LGPL-3.0-only

package com.hbm.lib.queues;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

import static com.hbm.lib.internal.UnsafeHolder.U;

public final class MpscUnboundedXaddArrayLongQueue
        extends MpUnboundedXaddArrayLongQueue<MpscUnboundedXaddChunkLong>
        implements LongMessagePassingQueue {

    public static final long EMPTY = Long.MIN_VALUE;

    private static final int RECOMMENDED_OFFER_BATCH =
            Runtime.getRuntime().availableProcessors() * 4;

    public MpscUnboundedXaddArrayLongQueue(int chunkSize, int maxPooledChunks) {
        super(chunkSize, maxPooledChunks);
    }

    public MpscUnboundedXaddArrayLongQueue(int chunkSize) {
        this(chunkSize, 2);
    }

    @Override
    MpscUnboundedXaddChunkLong newChunk(
            long index, MpscUnboundedXaddChunkLong prev, int chunkSize, boolean pooled) {
        return new MpscUnboundedXaddChunkLong(index, prev, chunkSize, pooled);
    }

    public boolean offer(long v) {
        if (v == EMPTY) {
            throw new IllegalArgumentException("Long.MIN_VALUE is reserved as EMPTY sentinel");
        }

        final int chunkMask = this.chunkMask;
        final int chunkShift = this.chunkShift;

        final long pIndex = U.getAndAddLong(this, P_INDEX_OFFSET, 1L);

        final int piChunkOffset = (int) (pIndex & chunkMask);
        final long piChunkIndex = pIndex >> chunkShift;

        MpscUnboundedXaddChunkLong pChunk = producerChunk;
        if (pChunk.index != piChunkIndex) {
            pChunk = producerChunkForIndex(pChunk, piChunkIndex);
        }
        U.putLongRelease(
                pChunk.buffer, MpUnboundedXaddChunkLong.calcLongElementOffset(piChunkOffset), v);
        return true;
    }

    @Override
    public long poll() {
        final int chunkMask = this.chunkMask;
        final long cIndex = U.getLong(this, C_INDEX_OFFSET);
        final int ciChunkOffset = (int) (cIndex & chunkMask);

        MpscUnboundedXaddChunkLong cChunk = consumerChunk;
        if (ciChunkOffset == 0 && cIndex != 0) {
            cChunk = pollNextBuffer(cChunk, cIndex);
            if (cChunk == null) {
                return EMPTY;
            }
        }

        long e =
                U.getLongAcquire(
                        cChunk.buffer,
                        MpUnboundedXaddChunkLong.calcLongElementOffset(ciChunkOffset));
        if (e == EMPTY) {
            if (producerIndex == cIndex) {
                return EMPTY;
            } else {
                e = cChunk.spinForElement(ciChunkOffset);
            }
        }
        U.putLongRelease(
                cChunk.buffer,
                MpUnboundedXaddChunkLong.calcLongElementOffset(ciChunkOffset),
                EMPTY);
        U.putLongRelease(this, C_INDEX_OFFSET, cIndex + 1);
        return e;
    }

    @Override
    public long peek() {
        final int chunkMask = this.chunkMask;
        final long cIndex = U.getLong(this, C_INDEX_OFFSET);
        final int ciChunkOffset = (int) (cIndex & chunkMask);

        MpscUnboundedXaddChunkLong cChunk =
                (MpscUnboundedXaddChunkLong) U.getReference(this, C_CHUNK_OFFSET);
        if (ciChunkOffset == 0 && cIndex != 0) {
            cChunk = spinForNextIfNotEmpty(cChunk, cIndex);
            if (cChunk == null) {
                return EMPTY;
            }
        }

        long e =
                U.getLongAcquire(
                        cChunk.buffer,
                        MpUnboundedXaddChunkLong.calcLongElementOffset(ciChunkOffset));
        if (e == EMPTY) {
            if (producerIndex == cIndex) {
                return EMPTY;
            } else {
                e = cChunk.spinForElement(ciChunkOffset);
            }
        }
        return e;
    }

    @Override
    public long relaxedPoll() {
        final int chunkMask = this.chunkMask;
        final long cIndex = U.getLong(this, C_INDEX_OFFSET);
        final int ciChunkOffset = (int) (cIndex & chunkMask);

        MpscUnboundedXaddChunkLong cChunk =
                (MpscUnboundedXaddChunkLong) U.getReference(this, C_CHUNK_OFFSET);
        long e;

        if (ciChunkOffset == 0 && cIndex != 0) {
            final MpscUnboundedXaddChunkLong next = cChunk.next;
            if (next == null) {
                return EMPTY;
            }
            e = U.getLongAcquire(next.buffer, MpUnboundedXaddChunkLong.calcLongElementOffset(0));

            if (e == EMPTY) {
                return EMPTY;
            }
            moveToNextConsumerChunk(cChunk, next);
            cChunk = next;
        } else {
            e =
                    U.getLongAcquire(
                            cChunk.buffer,
                            MpUnboundedXaddChunkLong.calcLongElementOffset(ciChunkOffset));
            if (e == EMPTY) {
                return EMPTY;
            }
        }

        U.putLongRelease(
                cChunk.buffer,
                MpUnboundedXaddChunkLong.calcLongElementOffset(ciChunkOffset),
                EMPTY);
        U.putLongRelease(this, C_INDEX_OFFSET, cIndex + 1);
        return e;
    }

    @Override
    public long relaxedPeek() {
        final int chunkMask = this.chunkMask;
        final long cIndex = U.getLong(this, C_INDEX_OFFSET);
        final int cChunkOffset = (int) (cIndex & chunkMask);

        MpscUnboundedXaddChunkLong cChunk =
                (MpscUnboundedXaddChunkLong) U.getReference(this, C_CHUNK_OFFSET);
        if (cChunkOffset == 0 && cIndex != 0) {
            cChunk = cChunk.next;
            if (cChunk == null) {
                return EMPTY;
            }
        }
        return U.getLongAcquire(
                cChunk.buffer, MpUnboundedXaddChunkLong.calcLongElementOffset(cChunkOffset));
    }

    public int drain(LongConsumer c, int limit) {
        if (c == null) throw new IllegalArgumentException("c is null");
        if (limit < 0) throw new IllegalArgumentException("limit is negative: " + limit);
        if (limit == 0) return 0;

        final int chunkMask = this.chunkMask;

        long cIndex = U.getLong(this, C_INDEX_OFFSET);
        MpscUnboundedXaddChunkLong cChunk =
                (MpscUnboundedXaddChunkLong) U.getReference(this, C_CHUNK_OFFSET);

        for (int i = 0; i < limit; i++) {
            final int consumerOffset = (int) (cIndex & chunkMask);

            long e;
            if (consumerOffset == 0 && cIndex != 0) {
                final MpscUnboundedXaddChunkLong next = cChunk.next;
                if (next == null) {
                    return i;
                }
                e =
                        U.getLongAcquire(
                                next.buffer, MpUnboundedXaddChunkLong.calcLongElementOffset(0));

                if (e == EMPTY) {
                    return i;
                }
                moveToNextConsumerChunk(cChunk, next);
                cChunk = next;
            } else {
                e =
                        U.getLongAcquire(
                                cChunk.buffer,
                                MpUnboundedXaddChunkLong.calcLongElementOffset(consumerOffset));
                if (e == EMPTY) {
                    return i;
                }
            }

            U.putLongRelease(
                    cChunk.buffer,
                    MpUnboundedXaddChunkLong.calcLongElementOffset(consumerOffset),
                    EMPTY);
            final long nextConsumerIndex = cIndex + 1;
            U.putLongRelease(this, C_INDEX_OFFSET, nextConsumerIndex);
            c.accept(e);
            cIndex = nextConsumerIndex;
        }
        return limit;
    }

    @Override
    public int drain(LongConsumer consumer) {
        if (consumer == null) throw new IllegalArgumentException("consumer is null");
        int total = 0;
        int drained;
        do {
            drained = drain(consumer, RECOMMENDED_OFFER_BATCH);
            total += drained;
        } while (drained == RECOMMENDED_OFFER_BATCH
                && total <= Integer.MAX_VALUE - RECOMMENDED_OFFER_BATCH);
        return total;
    }

    public int fill(LongSupplier s) {
        if (s == null) throw new IllegalArgumentException("supplier is null");

        long result = 0;
        final int capacity = chunkMask + 1;
        final int offerBatch = Math.min(RECOMMENDED_OFFER_BATCH, capacity);
        do {
            final int filled = fill(s, offerBatch);
            if (filled == 0) {
                return (int) result;
            }
            result += filled;
        } while (result <= capacity);
        return (int) result;
    }

    public int fill(LongSupplier s, int limit) {
        if (s == null) throw new IllegalArgumentException("supplier is null");
        if (limit < 0) throw new IllegalArgumentException("limit is negative:" + limit);
        if (limit == 0) return 0;

        for (int i = 0; i < limit; i++) {
            offer(s.getAsLong());
        }
        return limit;
    }

    @Override
    public int fill(long[] source, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, source.length);
        if (length == 0) return 0;
        if (length == 1) {
            offer(source[offset]);
            return 1;
        }
        for (int i = 0; i < length; i++) {
            if (source[offset + i] == EMPTY) {
                throw new IllegalArgumentException(
                        "Long.MIN_VALUE is reserved as EMPTY sentinel at source index "
                                + (offset + i));
            }
        }

        final long firstIndex = U.getAndAddLong(this, P_INDEX_OFFSET, length);
        MpscUnboundedXaddChunkLong chunk = producerChunk;
        long chunkIndex = chunk.index;
        for (int i = 0; i < length; i++) {
            final long index = firstIndex + i;
            final long requiredChunkIndex = index >> chunkShift;
            if (chunkIndex != requiredChunkIndex) {
                chunk = producerChunkForIndex(chunk, requiredChunkIndex);
                chunkIndex = chunk.index;
            }
            U.putLongRelease(
                    chunk.buffer,
                    MpUnboundedXaddChunkLong.calcLongElementOffset((int) (index & chunkMask)),
                    source[offset + i]);
        }
        return length;
    }

    @Override
    public int size() {
        long after = consumerIndex;
        while (true) {
            long before = after;
            long producer = producerIndex;
            after = consumerIndex;
            if (before == after) {
                long size = producer - after;
                if (size <= 0L) return 0;
                return size >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
            }
            Thread.onSpinWait();
        }
    }

    @Override
    public long currentProducerIndex() {
        return producerIndex;
    }

    @Override
    public long currentConsumerIndex() {
        return consumerIndex;
    }

    public boolean isEmpty() {
        final long cIndex = consumerIndex;
        return cIndex >= producerIndex;
    }

    public void clear() {
        while (poll() != EMPTY) {}
    }

    public void clear(boolean scrub) {
        final long pIndex = this.producerIndex;
        final MpscUnboundedXaddChunkLong pChunk = this.producerChunk;

        MpscUnboundedXaddChunkLong chunk = this.consumerChunk;
        while (chunk != pChunk) {
            final MpscUnboundedXaddChunkLong next = chunk.next;
            U.putReference(chunk, MpUnboundedXaddChunkLong.PREV_OFFSET, null);
            U.putReference(chunk, MpUnboundedXaddChunkLong.NEXT_OFFSET, null);
            if (chunk.pooled) {
                Arrays.fill(chunk.buffer, EMPTY);
                final boolean pooled = freeChunksPool.offer(chunk);
                assert pooled;
            }
            chunk = next;
        }

        final int used = pIndex == 0 ? 0 : (int) (((pIndex - 1) & this.chunkMask) + 1);
        if (scrub || pChunk.pooled) {
            Arrays.fill(pChunk.buffer, 0, used, EMPTY);
        }
        U.putReference(pChunk, MpUnboundedXaddChunkLong.PREV_OFFSET, null);
        U.putReference(pChunk, MpUnboundedXaddChunkLong.NEXT_OFFSET, null);
        U.putReferenceRelease(this, C_CHUNK_OFFSET, pChunk);
        U.putLongRelease(this, C_INDEX_OFFSET, pIndex);
    }

    private MpscUnboundedXaddChunkLong pollNextBuffer(
            MpscUnboundedXaddChunkLong cChunk, long cIndex) {
        final MpscUnboundedXaddChunkLong next = spinForNextIfNotEmpty(cChunk, cIndex);
        if (next == null) {
            return null;
        }
        moveToNextConsumerChunk(cChunk, next);
        assert next.index == (cIndex >> chunkShift);
        return next;
    }

    private MpscUnboundedXaddChunkLong spinForNextIfNotEmpty(
            MpscUnboundedXaddChunkLong cChunk, long cIndex) {
        MpscUnboundedXaddChunkLong next = cChunk.next;
        if (next == null) {
            if (producerIndex == cIndex) {
                return null;
            }
            final long ccChunkIndex = cChunk.index;
            if (producerChunkIndex == ccChunkIndex) {

                next = appendNextChunks(cChunk, ccChunkIndex, 1);
            }
            while (next == null) {
                Thread.onSpinWait();
                next = cChunk.next;
            }
        }
        return next;
    }
}
