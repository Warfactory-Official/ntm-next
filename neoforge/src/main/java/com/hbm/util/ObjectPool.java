// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ObjectPool<T> {
    private final Object[] pool;
    private final Supplier<? extends T> factory;
    private final Consumer<? super T> reset;
    private int size;

    public ObjectPool(Supplier<? extends T> factory, Consumer<? super T> reset, int cap) {
        this.factory = Objects.requireNonNull(factory);
        this.reset = Objects.requireNonNull(reset);
        if (cap < 1) throw new IllegalArgumentException("cap must be >= 1");
        this.pool = new Object[cap];
    }

    @SuppressWarnings("unchecked")
    public T borrow() {
        if (size != 0) {
            int index = --size;
            T t = (T) pool[index];
            pool[index] = null;
            return t;
        }
        T t = factory.get();
        if (t == null) throw new NullPointerException();
        return t;
    }

    public void recycle(T t) {
        tryRecycle(t);
    }

    public boolean tryRecycle(T t) {
        if (t == null) throw new NullPointerException();
        reset.accept(t);
        if (size == pool.length) return false;
        pool[size++] = t;
        return true;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return pool.length;
    }

    public void clear() {
        Arrays.fill(pool, 0, size, null);
        size = 0;
    }

    public int trimTo(int max) {
        if (max < 0) throw new IllegalArgumentException("max < 0");
        int target = Math.min(size, max);
        int removed = size - target;
        Arrays.fill(pool, target, size, null);
        size = target;
        return removed;
    }
}
