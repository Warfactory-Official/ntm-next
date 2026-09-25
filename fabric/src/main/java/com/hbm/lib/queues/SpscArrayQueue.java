// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: JCTools contributors
// SPDX-License-Identifier: Apache-2.0 AND LGPL-3.0-only

package com.hbm.lib.queues;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.HashCommon;

import static com.hbm.lib.internal.UnsafeHolder.U;
import static com.hbm.lib.internal.UnsafeHolder.fieldOffset;
import static com.hbm.lib.queues.RefArrayAccess.*;

abstract class SpscArrayQueuePad0 {
    @SuppressWarnings("unused")
    long p00, p01, p02, p03, p04, p05, p06;
}

abstract class SpscArrayQueueColdFields<E> extends SpscArrayQueuePad0 {
    protected final long mask;
    protected final E[] buffer;
    protected final int lookAheadStep;

    SpscArrayQueueColdFields(int actualCapacity) {
        this.mask = actualCapacity - 1;
        this.buffer = allocateRefArray(actualCapacity);
        this.lookAheadStep = Math.max(1, Math.min(actualCapacity / 4, 4096));
    }
}

abstract class SpscArrayQueuePad1<E> extends SpscArrayQueueColdFields<E> {
    @SuppressWarnings("unused")
    long p10, p11, p12, p13, p14, p15, p16;

    SpscArrayQueuePad1(int actualCapacity) {
        super(actualCapacity);
    }
}

abstract class SpscArrayQueueProducerFields<E> extends SpscArrayQueuePad1<E> {
    static final long P_INDEX_OFFSET =
            fieldOffset(SpscArrayQueueProducerFields.class, "producerIndex");

    protected long producerLimit;
    private volatile long producerIndex;

    SpscArrayQueueProducerFields(int actualCapacity) {
        super(actualCapacity);
    }

    final long lvProducerIndex() {
        return producerIndex;
    }

    final long lpProducerIndex() {
        return U.getLong(this, P_INDEX_OFFSET);
    }

    final void soProducerIndex(long newValue) {
        U.putLongRelease(this, P_INDEX_OFFSET, newValue);
    }
}

abstract class SpscArrayQueuePad2<E> extends SpscArrayQueueProducerFields<E> {
    @SuppressWarnings("unused")
    long p20, p21, p22, p23, p24, p25, p26;

    SpscArrayQueuePad2(int actualCapacity) {
        super(actualCapacity);
    }
}

abstract class SpscArrayQueueConsumerFields<E> extends SpscArrayQueuePad2<E> {
    static final long C_INDEX_OFFSET =
            fieldOffset(SpscArrayQueueConsumerFields.class, "consumerIndex");
    private volatile long consumerIndex;

    SpscArrayQueueConsumerFields(int actualCapacity) {
        super(actualCapacity);
    }

    final long lvConsumerIndex() {
        return consumerIndex;
    }

    final long lpConsumerIndex() {
        return U.getLong(this, C_INDEX_OFFSET);
    }

    final void soConsumerIndex(long newValue) {
        U.putLongRelease(this, C_INDEX_OFFSET, newValue);
    }
}

abstract class SpscArrayQueuePad3<E> extends SpscArrayQueueConsumerFields<E> {
    @SuppressWarnings("unused")
    long p30, p31, p32, p33, p34, p35, p36;

    SpscArrayQueuePad3(int actualCapacity) {
        super(actualCapacity);
    }
}

public final class SpscArrayQueue<E> extends SpscArrayQueuePad3<E>
        implements MessagePassingQueue<E>, QueueProgressIndicators {

    static final int CACHE_LINE = 64;

    static {
        final long pIdx = SpscArrayQueueProducerFields.P_INDEX_OFFSET;
        final long cIdx = SpscArrayQueueConsumerFields.C_INDEX_OFFSET;
        if (Math.abs(pIdx - cIdx) < CACHE_LINE) {
            throw new AssertionError(
                    "SpscArrayQueue layout check failed: "
                            + "producerIndex @ "
                            + pIdx
                            + " and consumerIndex @ "
                            + cIdx
                            + " are less than "
                            + CACHE_LINE
                            + " bytes apart - padding is ineffective on this JVM layout.");
        }
    }

    public SpscArrayQueue(final int capacity) {
        Preconditions.checkArgument(
                capacity >= 0 && capacity <= (1 << 30),
                "capacity: %s (expected: 0..%s)",
                capacity,
                1 << 30);
        super(HashCommon.nextPowerOfTwo(Math.max(capacity, 4)));
    }

    public int capacity() {
        return (int) (mask + 1);
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

    public boolean isEmpty() {
        return lvConsumerIndex() >= lvProducerIndex();
    }

    public boolean offer(final E e) {
        if (null == e) throw new NullPointerException();
        final E[] buffer = this.buffer;
        final long mask = this.mask;
        final long producerIndex = lpProducerIndex();

        if (producerIndex >= producerLimit && !offerSlowPath(buffer, mask, producerIndex)) {
            return false;
        }
        final long offset = calcCircularRefElementOffset(producerIndex, mask);
        soRefElement(buffer, offset, e);
        soProducerIndex(producerIndex + 1);
        return true;
    }

    private boolean offerSlowPath(final E[] buffer, final long mask, final long producerIndex) {
        final int lookAheadStep = this.lookAheadStep;
        if (null
                == lvRefElement(
                        buffer,
                        calcCircularRefElementOffset(producerIndex + lookAheadStep, mask))) {
            producerLimit = producerIndex + lookAheadStep;
            return true;
        }
        return null == lvRefElement(buffer, calcCircularRefElementOffset(producerIndex, mask));
    }

    public E poll() {
        final long consumerIndex = lpConsumerIndex();
        final long offset = calcCircularRefElementOffset(consumerIndex, mask);
        final E[] buffer = this.buffer;
        final E e = lvRefElement(buffer, offset);
        if (null == e) return null;
        soRefElement(buffer, offset, null);
        soConsumerIndex(consumerIndex + 1);
        return e;
    }

    public E peek() {
        return lvRefElement(buffer, calcCircularRefElementOffset(lpConsumerIndex(), mask));
    }

    @Override
    public boolean relaxedOffer(E e) {
        return offer(e);
    }

    @Override
    public E relaxedPoll() {
        return poll();
    }

    @Override
    public E relaxedPeek() {
        return peek();
    }

    @Override
    public int drain(MessagePassingQueue.Consumer<E> consumer, int limit) {
        if (consumer == null) throw new IllegalArgumentException("consumer is null");
        if (limit < 0) throw new IllegalArgumentException("limit is negative: " + limit);
        final E[] buffer = this.buffer;
        final long mask = this.mask;
        final long start = lpConsumerIndex();
        for (int i = 0; i < limit; i++) {
            final long index = start + i;
            final long offset = calcCircularRefElementOffset(index, mask);
            final E value = lvRefElement(buffer, offset);
            if (value == null) return i;
            soRefElement(buffer, offset, null);
            soConsumerIndex(index + 1L);
            consumer.accept(value);
        }
        return limit;
    }

    @Override
    public int fill(MessagePassingQueue.Supplier<E> supplier, int limit) {
        if (supplier == null) throw new IllegalArgumentException("supplier is null");
        if (limit < 0) throw new IllegalArgumentException("limit is negative: " + limit);
        final E[] buffer = this.buffer;
        final long mask = this.mask;
        final int lookAheadStep = this.lookAheadStep;
        final long start = lpProducerIndex();
        for (int i = 0; i < limit; i++) {
            final long index = start + i;
            final long lookAheadIndex = index + lookAheadStep;
            if (null == lvRefElement(buffer, calcCircularRefElementOffset(lookAheadIndex, mask))) {
                int batch = Math.min(lookAheadStep, limit - i);
                for (int j = 0; j < batch; j++) {
                    final long batchIndex = index + j;
                    E value = supplier.get();
                    if (value == null) throw new NullPointerException("supplier returned null");
                    soRefElement(buffer, calcCircularRefElementOffset(batchIndex, mask), value);
                    soProducerIndex(batchIndex + 1L);
                }
                i += batch - 1;
            } else {
                final long offset = calcCircularRefElementOffset(index, mask);
                if (lvRefElement(buffer, offset) != null) return i;
                E value = supplier.get();
                if (value == null) throw new NullPointerException("supplier returned null");
                soRefElement(buffer, offset, value);
                soProducerIndex(index + 1L);
            }
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
