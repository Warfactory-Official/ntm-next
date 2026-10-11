// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import com.google.common.math.LongMath;
import it.unimi.dsi.fastutil.HashCommon;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public final class Distribution {
    private Distribution() {}

    public static int normalizedCursor(int cursor, int size) {
        return size <= 0 ? 0 : Mth.positiveModulo(cursor, size);
    }

    public static <E> void debit(
            Bucket<E> supplies, Total owed, int[] cursor, int cursorIndex, Apply<E> apply) {
        int count = supplies.size();
        if (count == 0 || owed.isZero()) return;
        Total weight = supplies.weight.set(supplies.total);
        int start = normalizedCursor(cursor[cursorIndex], count);
        for (int step = 0; step < count && !owed.isZero(); step++) {
            int index = (start + step) % count;
            long supply = supplies.amount(index);
            long take = weightedShare(owed, supply, weight, Math.min(supply, owed.limit()));
            weight.subtract(supply);
            if (take <= 0) continue;
            apply.give(supplies.endpoint(index), take);
            owed.subtract(take);
        }
        cursor[cursorIndex] = (start + 1) % count;
    }

    public static <E> void serveTiers(
            Bucket<E>[] tiers,
            Total budget,
            Total delivered,
            int @Nullable [] cursors,
            int cursorOffset,
            Apply<E> apply) {
        for (int t = tiers.length - 1; t >= 0 && !budget.isZero(); t--) {
            Bucket<E> tier = tiers[t];
            if (tier == null || tier.isEmpty()) continue;
            int count = tier.size();
            int start = cursors == null ? 0 : normalizedCursor(cursors[cursorOffset + t], count);
            Total weight = tier.weight.set(tier.total);
            for (int step = 0; step < count && !budget.isZero(); step++) {
                int index = (start + step) % count;
                long want = tier.amount(index);
                long offer = weightedShare(budget, want, weight, Math.min(want, budget.limit()));
                weight.subtract(want);
                if (offer <= 0) continue;
                long taken = Math.min(apply.give(tier.endpoint(index), offer), offer);
                if (taken <= 0) continue;
                budget.subtract(taken);
                delivered.add(taken);
            }
            if (cursors != null) cursors[cursorOffset + t] = (start + 1) % count;
        }
    }

    public static <E> void serveInFull(
            Bucket<E>[] tiers, Total budget, Total delivered, Apply<E> apply) {
        for (int t = tiers.length - 1; t >= 0 && !budget.isZero(); t--) {
            Bucket<E> tier = tiers[t];
            if (tier == null) continue;
            for (int i = 0, size = tier.size(); i < size && !budget.isZero(); i++) {
                long give = Math.min(tier.amount(i), budget.limit());
                if (give <= 0) continue;
                long taken = Math.min(apply.give(tier.endpoint(i), give), give);
                if (taken <= 0) continue;
                delivered.add(taken);
                budget.subtract(taken);
            }
        }
    }

    public static <E> void serve(
            @Nullable Bucket<E>[] tiers,
            Total supply,
            Total demand,
            Settlement scratch,
            Total delivered,
            int @Nullable [] cursors,
            int cursorOffset,
            Apply<E> apply) {
        Total budget = scratch.budget.setMin(supply, demand);
        if (budget.isZero()) return;
        if (cursors == null && budget.compareTo(demand) >= 0)
            serveInFull(tiers, budget, delivered, apply);
        else serveTiers(tiers, budget, delivered, cursors, cursorOffset, apply);
    }

    public static <E> long absorbAndDebit(
            @Nullable Bucket<E> transit,
            @Nullable Bucket<E> own,
            @Nullable Bucket<E>[] bufferProviders,
            @Nullable Bucket<E>[] bufferReceivers,
            Total bufferDemand,
            Total owed,
            Settlement scratch,
            Apply<E> deliver,
            Apply<E> debit,
            int[] cursors,
            int transitCursor,
            int ownCursor,
            int bufferCursor) {
        Total ownOwed =
                scratch.ownOwed.positiveDifference(owed, transit == null ? ZERO : transit.total);
        Total untaken = scratch.untaken.positiveDifference(own == null ? ZERO : own.total, ownOwed);
        Total absorbed = scratch.absorbed.set(0);
        if (!untaken.isZero() && !bufferDemand.isZero()) {
            serveTiers(bufferReceivers, scratch.budget.set(untaken), absorbed, null, 0, deliver);
            owed.add(absorbed);
        }
        debitFrom(transit, owed, scratch, cursors, transitCursor, debit);
        debitFrom(own, owed, scratch, cursors, ownCursor, debit);
        for (int t = 0; t < bufferProviders.length && !owed.isZero(); t++) {
            debitFrom(bufferProviders[t], owed, scratch, cursors, bufferCursor + t, debit);
        }
        return absorbed.limit();
    }

    private static <E> void debitFrom(
            @Nullable Bucket<E> providers,
            Total owed,
            Settlement scratch,
            int[] cursors,
            int cursorIndex,
            Apply<E> apply) {
        if (providers == null || owed.isZero()) return;
        Total budget = scratch.budget.setMin(owed, providers.total);
        Total charged = scratch.charged.set(budget);
        debit(providers, budget, cursors, cursorIndex, apply);
        owed.subtract(charged.subtract(budget));
    }

    private static final Total ZERO = new Total();

    public static final class Settlement {
        final Total budget = new Total();
        final Total charged = new Total();
        final Total ownOwed = new Total();
        final Total untaken = new Total();
        final Total absorbed = new Total();
    }

    public static long weightedShare(Total total, long part, Total whole, long cap) {
        if (total.wide == null && whole.wide == null)
            return weightedShare(total.value, part, whole.value, cap);
        if (part <= 0 || cap <= 0 || whole.isZero()) return 0;
        return total.exact()
                .multiply(BigInteger.valueOf(part))
                .divide(whole.exact())
                .min(BigInteger.valueOf(cap))
                .longValue();
    }

    public static long weightedShare(long total, long part, long whole, long cap) {
        if (total <= 0 || part <= 0 || whole <= 0 || cap <= 0) return 0;
        if (part >= whole) return Math.min(total, cap);
        if (total <= Long.MAX_VALUE / part) return Math.min((total * part) / whole, cap);
        long gcdTW = LongMath.gcd(total, whole);
        long reducedTotal = total / gcdTW;
        long reducedWhole = whole / gcdTW;
        long gcdPW = LongMath.gcd(part, reducedWhole);
        long reducedPart = part / gcdPW;
        reducedWhole /= gcdPW;
        if (reducedTotal <= Long.MAX_VALUE / reducedPart)
            return Math.min((reducedTotal * reducedPart) / reducedWhole, cap);
        return BigInteger.valueOf(reducedTotal)
                .multiply(BigInteger.valueOf(reducedPart))
                .divide(BigInteger.valueOf(reducedWhole))
                .min(BigInteger.valueOf(cap))
                .longValue();
    }

    @FunctionalInterface
    public interface Apply<E> {
        long give(E endpoint, long amount);
    }

    public static final class Total {
        private long value;
        private @Nullable BigInteger wide;

        public Total set(long amount) {
            value = amount;
            wide = null;
            return this;
        }

        public Total set(Total amount) {
            value = amount.value;
            wide = amount.wide;
            return this;
        }

        public Total setMin(Total a, Total b) {
            return set(a.compareTo(b) <= 0 ? a : b);
        }

        public Total add(long amount) {
            if (wide != null) wide = wide.add(BigInteger.valueOf(amount));
            else if (Long.MAX_VALUE - value < amount)
                wide = BigInteger.valueOf(value).add(BigInteger.valueOf(amount));
            else value += amount;
            return this;
        }

        public Total add(Total amount) {
            if (amount.wide == null) return add(amount.value);
            wide = exact().add(amount.wide);
            return this;
        }

        public Total subtract(long amount) {
            if (wide == null) value -= amount;
            else narrow(wide.subtract(BigInteger.valueOf(amount)));
            assert wide != null || value >= 0;
            return this;
        }

        public Total subtract(Total amount) {
            if (amount.wide == null) return subtract(amount.value);
            narrow(exact().subtract(amount.wide));
            assert wide != null || value >= 0;
            return this;
        }

        public Total positiveDifference(Total a, Total b) {
            return a.compareTo(b) <= 0 ? set(0) : set(a).subtract(b);
        }

        public Total fraction(Total amount, double fraction) {
            if (amount.wide == null) return set((long) (amount.value * fraction));
            narrow(
                    new BigDecimal(amount.wide)
                            .multiply(BigDecimal.valueOf(fraction))
                            .toBigInteger());
            return this;
        }

        public boolean isZero() {
            return wide == null && value == 0;
        }

        public long limit() {
            return wide == null ? value : Long.MAX_VALUE;
        }

        public int compareTo(Total other) {
            if (wide == null && other.wide == null) return Long.compare(value, other.value);
            if (wide == null) return -1;
            if (other.wide == null) return 1;
            return wide.compareTo(other.wide);
        }

        public long difference(Total other) {
            if (wide == null && other.wide == null) return value - other.value;
            return exact().subtract(other.exact())
                    .max(BigInteger.valueOf(-Long.MAX_VALUE))
                    .min(BigInteger.valueOf(Long.MAX_VALUE))
                    .longValue();
        }

        private BigInteger exact() {
            return wide == null ? BigInteger.valueOf(value) : wide;
        }

        private void narrow(BigInteger amount) {
            if (amount.bitLength() < 64) {
                value = amount.longValue();
                wide = null;
            } else wide = amount;
        }
    }

    public static final class IdentitySet<E> {
        private Object[] keys = new Object[16];
        private int[] occupied = new int[8];
        private int size;

        public boolean add(E value) {
            int mask = keys.length - 1;
            int slot = HashCommon.mix(System.identityHashCode(value)) & mask;
            while (keys[slot] != null) {
                if (keys[slot] == value) return false;
                slot = (slot + 1) & mask;
            }
            if (size == occupied.length) {
                Object[] oldKeys = keys;
                int[] oldOccupied = occupied;
                keys = new Object[oldKeys.length * 2];
                occupied = new int[oldOccupied.length * 2];
                mask = keys.length - 1;
                for (int i = 0; i < size; i++) {
                    Object old = oldKeys[oldOccupied[i]];
                    int at = HashCommon.mix(System.identityHashCode(old)) & mask;
                    while (keys[at] != null) at = (at + 1) & mask;
                    keys[at] = old;
                    occupied[i] = at;
                }
                slot = HashCommon.mix(System.identityHashCode(value)) & mask;
                while (keys[slot] != null) slot = (slot + 1) & mask;
            }
            keys[slot] = value;
            occupied[size++] = slot;
            return true;
        }

        public void clear() {
            for (int i = 0; i < size; i++) keys[occupied[i]] = null;
            size = 0;
        }
    }

    public static final class Bucket<E> {
        private Object[] endpoints = new Object[4];
        private long[] amounts = new long[4];
        private int size;
        public final Total total = new Total();
        public final Total weight = new Total();

        public void add(E endpoint, long amount) {
            if (size == endpoints.length) {
                endpoints = Arrays.copyOf(endpoints, size * 2);
                amounts = Arrays.copyOf(amounts, size * 2);
            }
            endpoints[size] = endpoint;
            amounts[size++] = amount;
            total.add(amount);
        }

        @SuppressWarnings("unchecked")
        public E endpoint(int index) {
            return (E) endpoints[index];
        }

        public long amount(int index) {
            return amounts[index];
        }

        public void amount(int index, long amount) {
            total.subtract(amounts[index]).add(amount);
            amounts[index] = amount;
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public void clear() {
            Arrays.fill(endpoints, 0, size, null);
            size = 0;
            total.set(0);
        }
    }
}
