// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: JCTools contributors
// SPDX-License-Identifier: Apache-2.0 AND LGPL-3.0-only

package com.hbm.lib.queues;

import static com.hbm.lib.internal.UnsafeHolder.U;

public final class RefArrayAccess {
    public static final long REF_ARRAY_BASE;
    public static final int REF_ELEMENT_SHIFT;

    static {
        final int scale = U.arrayIndexScale(Object[].class);
        if (scale == 4) {
            REF_ELEMENT_SHIFT = 2;
        } else if (scale == 8) {
            REF_ELEMENT_SHIFT = 3;
        } else {
            throw new IllegalStateException("Unknown pointer size: " + scale);
        }
        REF_ARRAY_BASE = U.arrayBaseOffset(Object[].class);
    }

    private RefArrayAccess() {}

    public static <E> void soRefElement(E[] buffer, long offset, E e) {
        U.putReferenceRelease(buffer, offset, e);
    }

    @SuppressWarnings("unchecked")
    public static <E> E lvRefElement(E[] buffer, long offset) {
        return (E) U.getReferenceVolatile(buffer, offset);
    }

    public static long calcCircularRefElementOffset(long index, long mask) {
        return REF_ARRAY_BASE + ((index & mask) << REF_ELEMENT_SHIFT);
    }

    @SuppressWarnings("unchecked")
    public static <E> E[] allocateRefArray(int capacity) {
        return (E[]) new Object[capacity];
    }
}
