// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class SyncList<E> extends ArrayList<E> implements SyncSource {
    public SyncList() {}

    public SyncList(Collection<? extends E> values) {
        super(values);
    }

    @Override
    public E set(int index, E value) {
        E previous = super.set(index, value);
        if (previous != value) {
            if (syncBound()) {
                SyncBindings.unbind(this, previous);
                SyncBindings.bind(this, value, 3);
            }
            syncChanged(3);
        }
        return previous;
    }

    @Override
    public boolean add(E value) {
        add(size(), value);
        return true;
    }

    @Override
    public void addFirst(E value) {
        add(0, value);
    }

    @Override
    public void addLast(E value) {
        add(value);
    }

    @Override
    public E removeFirst() {
        if (isEmpty()) throw new NoSuchElementException();
        return remove(0);
    }

    @Override
    public E removeLast() {
        if (isEmpty()) throw new NoSuchElementException();
        return remove(size() - 1);
    }

    @Override
    public void add(int index, E value) {
        super.add(index, value);
        if (syncBound()) SyncBindings.bind(this, value, 3);
        syncChanged(3);
    }

    @Override
    public boolean addAll(Collection<? extends E> values) {
        return addAll(size(), values);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> values) {
        int before = size();
        if (!super.addAll(index, values)) return false;
        if (syncBound()) {
            for (int i = index, end = index + size() - before; i < end; i++)
                SyncBindings.bind(this, get(i), 3);
        }
        syncChanged(3);
        return true;
    }

    @Override
    public E remove(int index) {
        E previous = super.remove(index);
        if (syncBound()) SyncBindings.unbind(this, previous);
        syncChanged(3);
        return previous;
    }

    @Override
    public boolean remove(Object value) {
        int index = indexOf(value);
        if (index < 0) return false;
        remove(index);
        return true;
    }

    @Override
    public void clear() {
        if (isEmpty()) return;
        if (syncBound()) for (int i = 0; i < size(); i++) SyncBindings.unbind(this, get(i));
        super.clear();
        syncChanged(3);
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        boolean removed = false;
        int expected = modCount;
        for (int i = 0; i < size(); ) {
            boolean matches = filter.test(get(i));
            if (modCount != expected) throw new ConcurrentModificationException();
            if (matches) {
                remove(i);
                expected = modCount;
                removed = true;
            } else i++;
        }
        return removed;
    }

    @Override
    public boolean removeAll(Collection<?> values) {
        return removeIf(values::contains);
    }

    @Override
    public boolean retainAll(Collection<?> values) {
        Objects.requireNonNull(values);
        return removeIf(value -> !values.contains(value));
    }

    @Override
    public void replaceAll(UnaryOperator<E> operation) {
        Objects.requireNonNull(operation);
        int expected = modCount;
        for (int i = 0; i < size(); i++) {
            E value = operation.apply(get(i));
            if (modCount != expected) throw new ConcurrentModificationException();
            set(i, value);
        }
        modCount++;
    }

    @Override
    public void sort(Comparator<? super E> comparator) {
        super.sort(comparator);
        if (size() > 1) syncChanged(3);
    }

    @Override
    public Object clone() {
        return new SyncList<>(this);
    }

    @Override
    public List<E> subList(int from, int to) {
        Objects.checkFromToIndex(from, to, size());
        return new AbstractList<>() {
            private int length = to - from;
            private int expected = SyncList.this.modCount;

            private void check() {
                if (expected != SyncList.this.modCount) throw new ConcurrentModificationException();
            }

            @Override
            public int size() {
                check();
                return length;
            }

            @Override
            public E get(int index) {
                check();
                Objects.checkIndex(index, length);
                return SyncList.this.get(from + index);
            }

            @Override
            public E set(int index, E value) {
                check();
                Objects.checkIndex(index, length);
                return SyncList.this.set(from + index, value);
            }

            @Override
            public void add(int index, E value) {
                check();
                Objects.checkIndex(index, length + 1);
                SyncList.this.add(from + index, value);
                length++;
                expected = SyncList.this.modCount;
                modCount++;
            }

            @Override
            public E remove(int index) {
                check();
                Objects.checkIndex(index, length);
                E previous = SyncList.this.remove(from + index);
                length--;
                expected = SyncList.this.modCount;
                modCount++;
                return previous;
            }
        };
    }
}
