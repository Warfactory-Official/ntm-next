// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import com.google.common.math.LongMath;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public final class Distribution {

    private Distribution() {}

    public static int normalizedCursor(int cursor, int size) {
        return size <= 0 ? 0 : Mth.positiveModulo(cursor, size);
    }

    public static <E> void debit(
            @Nullable List<Share<E>> supplies,
            long owed,
            long totalSupply,
            int[] cursor,
            int cursorIndex,
            Apply<E> apply) {
        if (supplies == null) return;
        int count = supplies.size();
        if (count == 0 || owed <= 0) return;

        long remaining = owed;
        long remainingSupply = totalSupply;
        int start = normalizedCursor(cursor[cursorIndex], count);
        for (int step = 0; step < count && remaining > 0; step++) {
            Share<E> share = supplies.get((start + step) % count);
            long supply = share.amount();
            long cap = Math.min(supply, remaining);
            if (cap <= 0) {
                remainingSupply -= supply;
                continue;
            }
            long take =
                    (step == count - 1)
                            ? cap
                            : weightedShare(remaining, supply, remainingSupply, cap);
            if (take <= 0) {
                remainingSupply -= supply;
                continue;
            }
            apply.give(share.endpoint(), take);
            remaining -= take;
            remainingSupply -= supply;
        }
        cursor[cursorIndex] = (start + 1) % count;
    }

    public static long weightedShare(long total, long part, long whole, long cap) {
        if (total <= 0 || part <= 0 || whole <= 0 || cap <= 0) return 0;
        if (part >= whole) return Math.min(total, cap);
        if (total <= Long.MAX_VALUE / part) {
            long share = (total * part) / whole;
            return share <= 0 ? 0 : Math.min(share, cap);
        }
        long reducedTotal = total, reducedPart = part, reducedWhole = whole;
        long gcdTW = LongMath.gcd(reducedTotal, reducedWhole);
        reducedTotal /= gcdTW;
        reducedWhole /= gcdTW;
        long gcdPW = LongMath.gcd(reducedPart, reducedWhole);
        reducedPart /= gcdPW;
        reducedWhole /= gcdPW;
        long share;
        if (reducedTotal <= Long.MAX_VALUE / reducedPart) {
            share = (reducedTotal * reducedPart) / reducedWhole;
        } else {
            BigInteger exact =
                    BigInteger.valueOf(reducedTotal)
                            .multiply(BigInteger.valueOf(reducedPart))
                            .divide(BigInteger.valueOf(reducedWhole));
            if (exact.signum() <= 0) return 0;
            if (exact.compareTo(BigInteger.valueOf(cap)) >= 0) return cap;
            share = exact.longValue();
        }
        return share <= 0 ? 0 : Math.min(share, cap);
    }

    @FunctionalInterface
    public interface Apply<E> {
        long give(E endpoint, long amount);
    }

    public record Share<E>(E endpoint, long amount) {}
}
