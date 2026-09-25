// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.lib.internal.UnsafeHolder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.function.IntConsumer;
import org.jspecify.annotations.NonNull;

import static com.hbm.lib.internal.UnsafeHolder.U;

public class MpscIntArrayListCollector {

    private static final long HEAD_OFF =
            UnsafeHolder.fieldOffset(MpscIntArrayListCollector.class, "head");
    private volatile Node head;
    private volatile int drainCapacityHint;

    private static int append(Node head, IntArrayList out) {
        int base = out.size();
        int appended = 0;
        for (Node node = head; node != null; node = node.next) {
            int count = node.values == null ? 1 : node.values.length;
            long required = (long) base + appended + count;
            if (required > Integer.MAX_VALUE) {
                throw new IllegalStateException(
                        "drained value count exceeds Java array limits: " + required);
            }
            if (node.values == null) {
                out.add(node.v);
            } else {
                for (int i = node.values.length - 1; i >= 0; i--) out.add(node.values[i]);
            }
            appended += count;
        }
        return appended;
    }

    public void push(int i) {
        pushNode(new Node(i, null));
    }

    public void pushBatch(@NonNull IntList values) {
        int size = values.size();
        if (size == 0) return;
        if (size == 1) {
            push(values.getInt(0));
            return;
        }
        pushBatchOwned(values.toIntArray());
    }

    public void pushBatchOwned(int @NonNull [] values) {
        if (values.length == 0) return;
        pushNode(new Node(0, values));
    }

    private void pushNode(Node node) {
        while (true) {
            Node h = head;
            node.next = h;
            if (U.compareAndSetReference(this, HEAD_OFF, h, node)) return;
        }
    }

    public @NonNull IntArrayList drain() {
        Node h = detach();
        if (h == null) return new IntArrayList(0);
        if (h.next == null && h.values == null) {
            IntArrayList out = new IntArrayList(1);
            out.add(h.v);
            drainCapacityHint = 1;
            return out;
        }
        IntArrayList out = new IntArrayList(drainCapacityHint);
        int drained = append(h, out);
        drainCapacityHint = drained;
        return out;
    }

    public int drainTo(@NonNull IntArrayList l) {
        Node h = detach();
        return h == null ? 0 : append(h, l);
    }

    public int drainTo(@NonNull IntConsumer consumer) {
        Node h = detach();
        if (h == null) return 0;
        long count = 0L;
        for (Node p = h; p != null; p = p.next) {
            long nextCount = count + (p.values == null ? 1L : p.values.length);
            if (nextCount > Integer.MAX_VALUE) {
                throw new IllegalStateException(
                        "drained value count exceeds Java array limits: " + nextCount);
            }
            count = nextCount;
            if (p.values == null) {
                consumer.accept(p.v);
            } else {
                for (int i = p.values.length - 1; i >= 0; i--) consumer.accept(p.values[i]);
            }
        }
        return (int) count;
    }

    private Node detach() {
        while (true) {
            Node h = head;
            if (h == null) return null;
            if (U.compareAndSetReference(this, HEAD_OFF, h, null)) return h;
        }
    }

    private static final class Node {
        private final int v;
        private final int[] values;
        private Node next;

        private Node(int v, int[] values) {
            this.v = v;
            this.values = values;
        }
    }
}
