// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: JCTools contributors
// SPDX-License-Identifier: Apache-2.0 AND LGPL-3.0-only

package com.hbm.lib.maps;

import com.hbm.lib.internal.UnsafeHolder;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import static com.hbm.lib.internal.UnsafeHolder.U;

public class ConcurrentAutoTable implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private static final long CAT_OFF = UnsafeHolder.fieldOffset(ConcurrentAutoTable.class, "_cat");
    private volatile CAT _cat = new CAT(0L);

    public void add(long x) {
        add_if(x);
    }

    public void decrement() {
        add_if(-1L);
    }

    public void increment() {
        add_if(1L);
    }

    public void set(long x) {
        CAT newcat = new CAT(x);
        while (!CAS_cat(_cat, newcat)) {
            Thread.onSpinWait();
        }
    }

    public long get() {
        return _cat.sum();
    }

    public int intValue() {
        return (int) _cat.sum();
    }

    public long longValue() {
        return _cat.sum();
    }

    public long estimate_get() {
        return _cat.estimate_sum();
    }

    public String toString() {
        return _cat.toString();
    }

    public void print() {
        _cat.print();
    }

    public int internal_size() {
        return _cat.internal_size();
    }

    private long add_if(long x) {
        return _cat.add_if(x);
    }

    private boolean CAS_cat(CAT oldcat, CAT newcat) {
        return U.compareAndSetReference(this, CAT_OFF, oldcat, newcat);
    }

    private static final class CAT implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private static final long VALUE_OFF = UnsafeHolder.fieldOffset(Cell.class, "value");

        private volatile long _base;
        private volatile long _fuzzy_sum_cache;
        private volatile long _fuzzy_time;
        private volatile Cell[] _cells;
        private transient ThreadLocal<Cell> _local;

        CAT(long init) {
            _base = init;
            _cells = new Cell[0];
            _local = new ThreadLocal<>();
        }

        long add_if(long x) {
            Cell cell = _local.get();
            if (cell == null) cell = register();
            long old = U.getLong(cell, VALUE_OFF);
            U.putLongRelease(cell, VALUE_OFF, old + x);
            return old;
        }

        private synchronized Cell register() {
            Cell cell = _local.get();
            if (cell != null) return cell;

            Thread thread = Thread.currentThread();
            Cell[] cells = _cells;
            for (Cell candidate : cells) {
                WeakReference<Thread> ownerReference = candidate.owner;
                Thread owner = ownerReference == null ? null : ownerReference.get();
                if (owner == thread) {
                    _local.set(candidate);
                    return candidate;
                }
                if (owner == null || !owner.isAlive()) {
                    _base += U.getLongAcquire(candidate, VALUE_OFF);
                    U.putLongRelease(candidate, VALUE_OFF, 0L);
                    candidate.owner = new WeakReference<>(thread);
                    _local.set(candidate);
                    return candidate;
                }
            }

            cell = new Cell(thread);
            Cell[] expanded = Arrays.copyOf(cells, cells.length + 1);
            expanded[cells.length] = cell;
            _cells = expanded;
            _local.set(cell);
            return cell;
        }

        long sum() {
            long sum = _base;
            Cell[] cells = _cells;
            for (Cell cell : cells) sum += U.getLongAcquire(cell, VALUE_OFF);
            return sum;
        }

        long estimate_sum() {
            if (_cells.length <= 64) return sum();
            long millis = System.currentTimeMillis();
            if (_fuzzy_time != millis) {
                _fuzzy_sum_cache = sum();
                _fuzzy_time = millis;
            }
            return _fuzzy_sum_cache;
        }

        int internal_size() {
            return _cells.length;
        }

        public String toString() {
            return Long.toString(sum());
        }

        void print() {
            Cell[] cells = _cells;
            System.out.print("[" + _base);
            for (Cell cell : cells) System.out.print("," + U.getLongAcquire(cell, VALUE_OFF));
            System.out.print("]");
        }

        @Serial
        private void readObject(ObjectInputStream input)
                throws IOException, ClassNotFoundException {
            input.defaultReadObject();
            _local = new ThreadLocal<>();
        }
    }

    private static final class Cell implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        private long p0, p1, p2, p3, p4, p5, p6;
        private long value;
        private long q0, q1, q2, q3, q4, q5, q6;
        private transient WeakReference<Thread> owner;

        Cell(Thread owner) {
            this.owner = new WeakReference<>(owner);
        }
    }
}
