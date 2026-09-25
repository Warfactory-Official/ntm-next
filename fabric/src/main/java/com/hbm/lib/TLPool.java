// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib;

import com.hbm.lib.queues.MpmcArrayQueue;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TLPool<T> {
    private final ThreadLocal<LocalCache<T>> local;
    private final MpmcArrayQueue<T> shared;

    private final Supplier<? extends T> factory;
    private final Consumer<? super T> reset;
    private final int localCap;

    public TLPool(
            Supplier<? extends T> factory, Consumer<? super T> reset, int localCap, int sharedCap) {
        this.factory = Objects.requireNonNull(factory);
        this.reset = Objects.requireNonNull(reset);
        if (localCap < 0) throw new IllegalArgumentException("localCap must be >= 0");
        if (sharedCap < 2) throw new IllegalArgumentException("sharedCap must be >= 2");
        this.localCap = localCap;
        this.shared = new MpmcArrayQueue<>(sharedCap);
        this.local = ThreadLocal.withInitial(() -> new LocalCache<>(this.localCap));
    }

    public T borrow() {
        if (localCap == 0 || Thread.currentThread().isVirtual()) {
            T sharedValue = shared.relaxedPoll();
            return sharedValue != null ? sharedValue : newInstance();
        }
        LocalCache<T> q = local.get();
        T t = q.poll();
        if (t != null) return t;

        int moved = 0;
        while (moved < localCap) {
            T s = shared.relaxedPoll();
            if (s == null) break;
            q.add(s);
            moved++;
        }

        t = q.poll();
        if (t != null) return t;

        return newInstance();
    }

    public void recycle(T t) {
        tryRecycle(t);
    }

    public boolean tryRecycle(T t) {
        if (t == null) throw new NullPointerException();
        reset.accept(t);
        if (localCap == 0 || Thread.currentThread().isVirtual()) return shared.relaxedOffer(t);
        LocalCache<T> q = local.get();
        if (q.add(t)) {
            return true;
        }
        return shared.relaxedOffer(t);
    }

    private T newInstance() {
        T value = factory.get();
        if (value == null) throw new NullPointerException();
        return value;
    }

    public void clearLocal() {
        local.remove();
    }

    public int localCapacity() {
        return localCap;
    }

    public int currentLocalSize() {
        if (localCap == 0 || Thread.currentThread().isVirtual()) return 0;
        return local.get().size();
    }

    public int sharedCapacity() {
        return shared.capacity();
    }

    public int sharedSize() {
        return shared.size();
    }

    public int trimSharedTo(int max) {
        if (max < 0) throw new IllegalArgumentException("max < 0");
        int toRemove = Math.max(0, shared.size() - max);
        int removed = 0;
        while (removed < toRemove) {
            if (shared.poll() == null) break;
            removed++;
        }
        return removed;
    }

    private static final class LocalCache<T> {
        private final Object[] elements;
        private T top;
        private int size;

        private LocalCache(int capacity) {
            elements = new Object[Math.max(0, capacity - 1)];
        }

        boolean add(T value) {
            if (top == null) {
                top = value;
                return true;
            }
            if (size == elements.length) return false;
            elements[size++] = top;
            top = value;
            return true;
        }

        @SuppressWarnings("unchecked")
        T poll() {
            T value = top;
            if (value == null) return null;
            if (size == 0) {
                top = null;
            } else {
                int index = --size;
                top = (T) elements[index];
                elements[index] = null;
            }
            return value;
        }

        int size() {
            return size + (top == null ? 0 : 1);
        }
    }
}
