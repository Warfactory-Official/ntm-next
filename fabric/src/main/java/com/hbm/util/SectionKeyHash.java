// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import it.unimi.dsi.fastutil.longs.LongHash;

public final class SectionKeyHash {
    public static final LongHash.Strategy STRATEGY =
            new LongHash.Strategy() {
                @Override
                public int hashCode(long k) {
                    return hash(k);
                }

                @Override
                public boolean equals(long a, long b) {
                    return a == b;
                }
            };

    private SectionKeyHash() {}

    public static int hash(long z) {
        z = (z ^ (z >>> 33)) * 0x62a9d9ed799705f5L;
        return (int) (((z ^ (z >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
    }
}
