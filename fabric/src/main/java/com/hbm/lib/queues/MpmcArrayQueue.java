// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: JCTools contributors
// SPDX-License-Identifier: Apache-2.0 AND LGPL-3.0-only

package com.hbm.lib.queues;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.HashCommon;

import static com.hbm.lib.internal.UnsafeHolder.*;
import static com.hbm.lib.queues.RefArrayAccess.allocateRefArray;
import static com.hbm.lib.queues.RefArrayAccess.calcCircularRefElementOffset;

abstract class MpmcArrayQueuePad0 {
    @SuppressWarnings("unused")
    long p00, p01, p02, p03, p04, p05, p06;
}

abstract class MpmcArrayQueueColdFields<E> extends MpmcArrayQueuePad0 {
    private static final boolean BLOCKED_SEQUENCE_LAYOUT =
            System.getProperty("java.vm.version", "").contains("jvmci");
    private static final int SEQUENCE_BUFFER_PAD =
            (int) (((64L - (IA_BASE & 63L)) & 63L) >> IA_SHIFT);
    private static final int PUBLISHED_SEQUENCE_STRIDE = 5;

    protected final long mask;
    protected final E[] buffer;
    protected final int[] freeSequenceBuffer;
    protected final int[] publishedSequenceBuffer;
    protected final int lookAheadStep;

    MpmcArrayQueueColdFields(int actualCapacity) {
        this.mask = actualCapacity - 1L;
        this.buffer = allocateRefArray(actualCapacity);
        final int sequenceLanes = actualCapacity << 1;
        final int[] sequenceBuffer =
                new int
                        [BLOCKED_SEQUENCE_LAYOUT
                                ? SEQUENCE_BUFFER_PAD + Math.max(32, sequenceLanes)
                                : sequenceLanes];
        this.freeSequenceBuffer = sequenceBuffer;
        this.publishedSequenceBuffer = sequenceBuffer;
        this.lookAheadStep =
                Math.max(2, Math.min(actualCapacity / 4, MpmcArrayQueue.MAX_LOOK_AHEAD_STEP));
        if (BLOCKED_SEQUENCE_LAYOUT) {
            for (int block = 0; block < actualCapacity; block += 16) {
                int physical = SEQUENCE_BUFFER_PAD + (block << 1);
                int lanes = Math.min(16, actualCapacity - block);
                for (int lane = 0; lane < lanes; lane++)
                    sequenceBuffer[physical + lane] = block + lane;
            }
        } else {
            for (int i = 0; i < actualCapacity; i++) sequenceBuffer[i] = i;
        }
    }

    protected static long calcFreeSequenceElementOffset(long index, long mask) {
        final long slot = index & mask;
        if (!BLOCKED_SEQUENCE_LAYOUT) return IA_BASE + (slot << IA_SHIFT);
        final long blockBase = (slot & ~15L) << 1;
        return IA_BASE
                + ((long) SEQUENCE_BUFFER_PAD << IA_SHIFT)
                + ((blockBase + (slot & 15L)) << IA_SHIFT);
    }

    protected static long calcPublishedSequenceElementOffset(long index, long mask) {
        if (!BLOCKED_SEQUENCE_LAYOUT) {
            return IA_BASE
                    + ((mask + 1L + ((index * PUBLISHED_SEQUENCE_STRIDE) & mask)) << IA_SHIFT);
        }
        final long slot = index & mask;
        final long blockBase = (slot & ~15L) << 1;
        return IA_BASE
                + ((long) SEQUENCE_BUFFER_PAD << IA_SHIFT)
                + ((blockBase + 16L + (slot & 15L)) << IA_SHIFT);
    }

    protected static long calcPublishedSequenceElementOffset(
            long index, long mask, long freeOffset) {
        if (BLOCKED_SEQUENCE_LAYOUT) return freeOffset + (16L << IA_SHIFT);
        return calcPublishedSequenceElementOffset(index, mask);
    }

    protected static long calcFreeSequenceElementOffset(
            long index, long mask, long publishedOffset) {
        if (BLOCKED_SEQUENCE_LAYOUT) return publishedOffset - (16L << IA_SHIFT);
        return calcFreeSequenceElementOffset(index, mask);
    }

    protected static int lvSequence(int[] buffer, long offset) {
        return U.getIntVolatile(buffer, offset);
    }

    protected static void soSequence(int[] buffer, long offset, int value) {
        U.putIntRelease(buffer, offset, value);
    }
}

abstract class MpmcArrayQueuePad1<E> extends MpmcArrayQueueColdFields<E> {
    @SuppressWarnings("unused")
    long p10, p11, p12, p13, p14, p15, p16;

    MpmcArrayQueuePad1(int actualCapacity) {
        super(actualCapacity);
    }
}

abstract class MpmcArrayQueueProducerFields<E> extends MpmcArrayQueuePad1<E> {
    static final long P_INDEX_OFFSET =
            fieldOffset(MpmcArrayQueueProducerFields.class, "producerIndex");
    private volatile long producerIndex;

    MpmcArrayQueueProducerFields(int actualCapacity) {
        super(actualCapacity);
    }

    final long lvProducerIndex() {
        return producerIndex;
    }

    final boolean casProducerIndex(long expect, long newValue) {
        return U.compareAndSetLong(this, P_INDEX_OFFSET, expect, newValue);
    }
}

abstract class MpmcArrayQueuePad2<E> extends MpmcArrayQueueProducerFields<E> {
    @SuppressWarnings("unused")
    long p20, p21, p22, p23, p24, p25, p26;

    MpmcArrayQueuePad2(int actualCapacity) {
        super(actualCapacity);
    }
}

abstract class MpmcArrayQueueConsumerFields<E> extends MpmcArrayQueuePad2<E> {
    static final long C_INDEX_OFFSET =
            fieldOffset(MpmcArrayQueueConsumerFields.class, "consumerIndex");
    private volatile long consumerIndex;

    MpmcArrayQueueConsumerFields(int actualCapacity) {
        super(actualCapacity);
    }

    final long lvConsumerIndex() {
        return consumerIndex;
    }

    final boolean casConsumerIndex(long expect, long newValue) {
        return U.compareAndSetLong(this, C_INDEX_OFFSET, expect, newValue);
    }
}

abstract class MpmcArrayQueuePad3<E> extends MpmcArrayQueueConsumerFields<E> {
    @SuppressWarnings("unused")
    long p30, p31, p32, p33, p34, p35, p36;

    MpmcArrayQueuePad3(int actualCapacity) {
        super(actualCapacity);
    }
}

public final class MpmcArrayQueue<E> extends MpmcArrayQueuePad3<E>
        implements MessagePassingQueue<E>, QueueProgressIndicators {

    public static final int MAX_LOOK_AHEAD_STEP =
            Integer.getInteger("hbm.mpmc.max.lookahead.step", 4096);
    static final int CACHE_LINE = 64;
    static final int MAX_CAPACITY = 1 << 29;

    static {
        final long pIdx = MpmcArrayQueueProducerFields.P_INDEX_OFFSET;
        final long cIdx = MpmcArrayQueueConsumerFields.C_INDEX_OFFSET;
        if (Math.abs(pIdx - cIdx) < CACHE_LINE) {
            throw new AssertionError(
                    "MpmcArrayQueue layout check failed: "
                            + "producerIndex @ "
                            + pIdx
                            + " and consumerIndex @ "
                            + cIdx
                            + " are less than "
                            + CACHE_LINE
                            + " bytes apart - padding is ineffective on this JVM layout.");
        }
    }

    public MpmcArrayQueue(final int capacity) {
        Preconditions.checkArgument(
                capacity >= 2 && capacity <= MAX_CAPACITY,
                "capacity: %s (expected: 2..%s)",
                capacity,
                MAX_CAPACITY);
        super(HashCommon.nextPowerOfTwo(capacity));
    }

    public int capacity() {
        return (int) (mask + 1);
    }

    public boolean isEmpty() {
        return lvConsumerIndex() >= lvProducerIndex();
    }

    public int size() {
        long after = lvConsumerIndex();
        long size;
        while (true) {
            final long before = after;
            final long currentProducerIndex = lvProducerIndex();
            after = lvConsumerIndex();
            if (before == after) {
                size = currentProducerIndex - after;
                break;
            }
            Thread.onSpinWait();
        }
        if (size < 0) return 0;
        final int cap = capacity();
        if (size > cap) return cap;
        return (int) size;
    }

    public boolean offer(final E e) {
        if (null == e) throw new NullPointerException();
        final long mask = this.mask;
        final long capacity = mask + 1L;
        final int[] freeBuffer = freeSequenceBuffer;
        final int[] publishedBuffer = publishedSequenceBuffer;

        long pIndex;
        long seqOffset;
        int seqDelta;
        long cIndex = Long.MIN_VALUE;
        do {
            pIndex = lvProducerIndex();
            seqOffset = calcFreeSequenceElementOffset(pIndex, mask);
            seqDelta = lvSequence(freeBuffer, seqOffset) - (int) pIndex;
            if (seqDelta < 0) {
                if (pIndex - capacity >= cIndex
                        && pIndex - capacity >= (cIndex = lvConsumerIndex())) {
                    return false;
                }
                seqDelta = 1;
            }
        } while (seqDelta > 0 || !casProducerIndex(pIndex, pIndex + 1));

        U.putReference(buffer, calcCircularRefElementOffset(pIndex, mask), e);
        soSequence(
                publishedBuffer,
                calcPublishedSequenceElementOffset(pIndex, mask),
                (int) (pIndex + 1));
        return true;
    }

    public E poll() {
        final int[] freeBuffer = freeSequenceBuffer;
        final int[] publishedBuffer = publishedSequenceBuffer;
        final long mask = this.mask;

        long cIndex;
        long seqOffset;
        int seqDelta;
        long pIndex = -1L;
        do {
            cIndex = lvConsumerIndex();
            seqOffset = calcPublishedSequenceElementOffset(cIndex, mask);
            seqDelta = lvSequence(publishedBuffer, seqOffset) - (int) (cIndex + 1L);
            if (seqDelta < 0) {
                if (cIndex >= pIndex && cIndex == (pIndex = lvProducerIndex())) {
                    return null;
                }
                seqDelta = 1;
            }
        } while (seqDelta > 0 || !casConsumerIndex(cIndex, cIndex + 1));

        final long offset = calcCircularRefElementOffset(cIndex, mask);
        @SuppressWarnings("unchecked")
        final E e = (E) U.getReference(buffer, offset);
        U.putReference(buffer, offset, null);
        soSequence(
                freeBuffer,
                calcFreeSequenceElementOffset(cIndex, mask),
                (int) (cIndex + mask + 1L));
        return e;
    }

    public boolean relaxedOffer(final E e) {
        if (null == e) throw new NullPointerException();
        final long mask = this.mask;
        final int[] freeBuffer = freeSequenceBuffer;
        final int[] publishedBuffer = publishedSequenceBuffer;

        long pIndex;
        long seqOffset;
        int seqDelta;
        do {
            pIndex = lvProducerIndex();
            seqOffset = calcFreeSequenceElementOffset(pIndex, mask);
            seqDelta = lvSequence(freeBuffer, seqOffset) - (int) pIndex;
            if (seqDelta < 0) return false;
        } while (seqDelta > 0 || !casProducerIndex(pIndex, pIndex + 1));

        U.putReference(buffer, calcCircularRefElementOffset(pIndex, mask), e);
        soSequence(
                publishedBuffer,
                calcPublishedSequenceElementOffset(pIndex, mask),
                (int) (pIndex + 1));
        return true;
    }

    public E relaxedPoll() {
        final int[] freeBuffer = freeSequenceBuffer;
        final int[] publishedBuffer = publishedSequenceBuffer;
        final long mask = this.mask;

        long cIndex;
        long seqOffset;
        int seqDelta;
        do {
            cIndex = lvConsumerIndex();
            seqOffset = calcPublishedSequenceElementOffset(cIndex, mask);
            seqDelta = lvSequence(publishedBuffer, seqOffset) - (int) (cIndex + 1L);
            if (seqDelta < 0) return null;
        } while (seqDelta > 0 || !casConsumerIndex(cIndex, cIndex + 1));

        final long offset = calcCircularRefElementOffset(cIndex, mask);
        @SuppressWarnings("unchecked")
        final E e = (E) U.getReference(buffer, offset);
        U.putReference(buffer, offset, null);
        soSequence(
                freeBuffer,
                calcFreeSequenceElementOffset(cIndex, mask),
                (int) (cIndex + mask + 1L));
        return e;
    }

    @Override
    public E peek() {
        final int[] publishedBuffer = publishedSequenceBuffer;
        final long mask = this.mask;
        long pIndex = -1L;
        while (true) {
            final long cIndex = lvConsumerIndex();
            final long seqOffset = calcPublishedSequenceElementOffset(cIndex, mask);
            final int seqDelta = lvSequence(publishedBuffer, seqOffset) - (int) (cIndex + 1L);
            if (seqDelta < 0) {
                if (cIndex >= pIndex && cIndex == (pIndex = lvProducerIndex())) return null;
            } else if (seqDelta == 0) {
                final long offset = calcCircularRefElementOffset(cIndex, mask);
                final E value = RefArrayAccess.lvRefElement(buffer, offset);
                if (lvConsumerIndex() == cIndex) return value;
            }
        }
    }

    @Override
    public E relaxedPeek() {
        final int[] publishedBuffer = publishedSequenceBuffer;
        final long mask = this.mask;
        while (true) {
            final long cIndex = lvConsumerIndex();
            final long seqOffset = calcPublishedSequenceElementOffset(cIndex, mask);
            final int seqDelta = lvSequence(publishedBuffer, seqOffset) - (int) (cIndex + 1L);
            if (seqDelta < 0) return null;
            if (seqDelta == 0) {
                final E value =
                        RefArrayAccess.lvRefElement(
                                buffer, calcCircularRefElementOffset(cIndex, mask));
                if (lvConsumerIndex() == cIndex) return value;
            }
        }
    }

    @Override
    public int drain(MessagePassingQueue.Consumer<E> consumer, int limit) {
        if (consumer == null) throw new IllegalArgumentException("consumer is null");
        if (limit < 0) throw new IllegalArgumentException("limit is negative: " + limit);
        if (limit == 0) return 0;

        final int[] freeBuffer = freeSequenceBuffer;
        final int[] publishedBuffer = publishedSequenceBuffer;
        final long mask = this.mask;
        final int maxLookAhead = Math.min(lookAheadStep, limit);
        int consumed = 0;
        while (consumed < limit) {
            final int remaining = limit - consumed;
            final int step = Math.min(remaining, maxLookAhead);
            final long cIndex = lvConsumerIndex();
            final long lookAheadIndex = cIndex + step - 1L;
            final long lookAheadOffset = calcPublishedSequenceElementOffset(lookAheadIndex, mask);
            final long expectedLookAhead = lookAheadIndex + 1L;
            final int lookAheadDelta =
                    lvSequence(publishedBuffer, lookAheadOffset) - (int) expectedLookAhead;
            if (lookAheadDelta == 0 && casConsumerIndex(cIndex, expectedLookAhead)) {
                for (int i = 0; i < step; i++) {
                    final long index = cIndex + i;
                    final long seqOffset = calcPublishedSequenceElementOffset(index, mask);
                    final int expectedSeq = (int) (index + 1L);
                    while (lvSequence(publishedBuffer, seqOffset) != expectedSeq) {}
                    final long offset = calcCircularRefElementOffset(index, mask);
                    @SuppressWarnings("unchecked")
                    final E value = (E) U.getReference(buffer, offset);
                    U.putReference(buffer, offset, null);
                    soSequence(
                            freeBuffer,
                            calcFreeSequenceElementOffset(index, mask, seqOffset),
                            (int) (index + mask + 1L));
                    consumer.accept(value);
                }
                consumed += step;
            } else {
                if (lookAheadDelta < 0
                        && lvSequence(
                                                publishedBuffer,
                                                calcPublishedSequenceElementOffset(cIndex, mask))
                                        - (int) (cIndex + 1L)
                                < 0) {
                    return consumed;
                }
                return consumed + drainOneByOne(consumer, remaining);
            }
        }
        return consumed;
    }

    private int drainOneByOne(MessagePassingQueue.Consumer<E> consumer, int limit) {
        for (int i = 0; i < limit; i++) {
            E value = relaxedPoll();
            if (value == null) return i;
            consumer.accept(value);
        }
        return limit;
    }

    @Override
    public int fill(MessagePassingQueue.Supplier<E> supplier, int limit) {
        if (supplier == null) throw new IllegalArgumentException("supplier is null");
        if (limit < 0) throw new IllegalArgumentException("limit is negative: " + limit);
        if (limit == 0) return 0;

        final int[] freeBuffer = freeSequenceBuffer;
        final int[] publishedBuffer = publishedSequenceBuffer;
        final long mask = this.mask;
        final int maxLookAhead = Math.min(lookAheadStep, limit);
        int produced = 0;
        while (produced < limit) {
            final int remaining = limit - produced;
            final int step = Math.min(remaining, maxLookAhead);
            final long pIndex = lvProducerIndex();
            final long lookAheadIndex = pIndex + step - 1L;
            final long lookAheadOffset = calcFreeSequenceElementOffset(lookAheadIndex, mask);
            final long expectedLookAhead = lookAheadIndex;
            final int lookAheadDelta =
                    lvSequence(freeBuffer, lookAheadOffset) - (int) expectedLookAhead;
            if (lookAheadDelta == 0 && casProducerIndex(pIndex, expectedLookAhead + 1L)) {
                for (int i = 0; i < step; i++) {
                    final long index = pIndex + i;
                    final long seqOffset = calcFreeSequenceElementOffset(index, mask);
                    while (lvSequence(freeBuffer, seqOffset) != (int) index) {}
                    final E value = supplier.get();
                    if (value == null) throw new NullPointerException("supplier returned null");
                    U.putReference(buffer, calcCircularRefElementOffset(index, mask), value);
                    soSequence(
                            publishedBuffer,
                            calcPublishedSequenceElementOffset(index, mask, seqOffset),
                            (int) (index + 1L));
                }
                produced += step;
            } else {
                if (lookAheadDelta < 0
                        && lvSequence(freeBuffer, calcFreeSequenceElementOffset(pIndex, mask))
                                        - (int) pIndex
                                < 0) {
                    return produced;
                }
                return produced + fillOneByOne(supplier, remaining);
            }
        }
        return produced;
    }

    private int fillOneByOne(MessagePassingQueue.Supplier<E> supplier, int limit) {
        final int[] freeBuffer = freeSequenceBuffer;
        final int[] publishedBuffer = publishedSequenceBuffer;
        final long mask = this.mask;
        for (int i = 0; i < limit; i++) {
            long pIndex;
            long seqOffset;
            int seqDelta;
            do {
                pIndex = lvProducerIndex();
                seqOffset = calcFreeSequenceElementOffset(pIndex, mask);
                seqDelta = lvSequence(freeBuffer, seqOffset) - (int) pIndex;
                if (seqDelta < 0) return i;
            } while (seqDelta > 0 || !casProducerIndex(pIndex, pIndex + 1L));

            final E value = supplier.get();
            if (value == null) throw new NullPointerException("supplier returned null");
            RefArrayAccess.soRefElement(buffer, calcCircularRefElementOffset(pIndex, mask), value);
            soSequence(
                    publishedBuffer,
                    calcPublishedSequenceElementOffset(pIndex, mask),
                    (int) (pIndex + 1L));
        }
        return limit;
    }

    @Override
    public int drain(MessagePassingQueue.Consumer<E> consumer) {
        return drain(consumer, capacity());
    }

    @Override
    public int fill(MessagePassingQueue.Supplier<E> supplier) {
        return fill(supplier, capacity());
    }

    @Override
    public void drain(
            MessagePassingQueue.Consumer<E> consumer,
            MessagePassingQueue.WaitStrategy wait,
            MessagePassingQueue.ExitCondition exit) {
        if (consumer == null || wait == null || exit == null) throw new NullPointerException();
        int idleCounter = 0;
        while (exit.keepRunning()) {
            E value = relaxedPoll();
            if (value == null) {
                idleCounter = wait.idle(idleCounter);
            } else {
                idleCounter = 0;
                consumer.accept(value);
            }
        }
    }

    @Override
    public void fill(
            MessagePassingQueue.Supplier<E> supplier,
            MessagePassingQueue.WaitStrategy wait,
            MessagePassingQueue.ExitCondition exit) {
        if (supplier == null || wait == null || exit == null) throw new NullPointerException();
        int idleCounter = 0;
        while (exit.keepRunning()) {
            int filled = fill(supplier, lookAheadStep);
            idleCounter = filled == 0 ? wait.idle(idleCounter) : 0;
        }
    }

    @Override
    public void clear() {
        while (poll() != null || !isEmpty()) {}
    }

    @Override
    public long currentProducerIndex() {
        return lvProducerIndex();
    }

    @Override
    public long currentConsumerIndex() {
        return lvConsumerIndex();
    }
}
