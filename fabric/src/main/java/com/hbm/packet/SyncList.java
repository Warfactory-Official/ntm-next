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

import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
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

    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
