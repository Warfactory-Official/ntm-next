// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.math.BigInteger;
import java.util.ArrayList;

public final class BufferPool<H> {

    public interface Port<H> {
        long stock(H handler);

        long headroom(H handler);

        long credit(H handler, long amount);

        void debit(H handler, long amount);
    }

    private static final class Member<H> {
        H handler;
        int tier;
        long gathered;
        long outSpeed;
        long inSpeed;
        long stock;
        long capacity;
        long out;
        long in;
        long give;
        long take;
    }

    private final ArrayList<Member<H>> members = new ArrayList<>();
    private final ReferenceOpenHashSet<H> seen = new ReferenceOpenHashSet<>();
    private int size;

    public void reset() {
        for (int i = 0; i < size; i++) members.get(i).handler = null;
        size = 0;
        seen.clear();
    }

    public void add(H handler, int tier, long stock, long outSpeed, long inSpeed) {
        if (!seen.add(handler)) return;
        Member<H> m;
        if (size < members.size()) {
            m = members.get(size);
        } else {
            members.add(m = new Member<>());
        }
        size++;
        m.handler = handler;
        m.tier = tier;
        m.gathered = stock;
        m.outSpeed = outSpeed;
        m.inSpeed = inSpeed;
    }

    public long exchange(Port<H> port, int tiers, long cap) {
        if (size < 2 || cap <= 0) return 0;
        int present = 0;
        for (int i = 0; i < size; i++) {
            Member<H> m = members.get(i);
            present |= 1 << m.tier;
            long stock = Math.max(0, port.stock(m.handler));
            long headroom = Math.max(0, port.headroom(m.handler));
            m.stock = stock;
            m.capacity = saturatedAdd(stock, headroom);

            m.out = Math.min(stock, Math.max(0, m.outSpeed - Math.max(0, m.gathered - stock)));
            m.in = Math.min(headroom, Math.max(0, m.inSpeed - Math.max(0, stock - m.gathered)));
        }
        long moved = 0;
        for (int high = tiers - 1; high > 0; high--) {
            if ((present & 1 << high) == 0) continue;
            for (int low = 0; low < high; low++) {
                if ((present & 1 << low) == 0) continue;
                long want = 0, offer = 0;
                for (int i = 0; i < size; i++) {
                    Member<H> m = members.get(i);
                    m.take = m.tier == high ? m.in : 0;
                    m.give = m.tier == low ? m.out : 0;
                    want = saturatedAdd(want, m.take);
                    offer = saturatedAdd(offer, m.give);
                }
                moved += transfer(port, Math.min(Math.min(want, offer), cap - moved), want, offer);
                if (moved >= cap) return moved;
            }
        }
        for (int t = 0; t < tiers; t++) {
            if ((present & 1 << t) == 0) continue;
            moved += level(port, t, cap - moved);
            if (moved >= cap) return moved;
        }
        return moved;
    }

    private long level(Port<H> port, int tier, long cap) {
        long sumStock = 0, sumCapacity = 0;
        boolean wide = false;
        int count = 0;
        for (int i = 0; i < size; i++) {
            Member<H> m = members.get(i);
            if (m.tier != tier) continue;
            count++;
            sumStock += m.stock;
            sumCapacity += m.capacity;
            if (sumStock < 0 || sumCapacity < 0) wide = true;
        }
        if (count < 2) return 0;
        BigInteger wideStock = null, wideCapacity = null;
        if (wide) {
            wideStock = wideCapacity = BigInteger.ZERO;
            for (int i = 0; i < size; i++) {
                Member<H> m = members.get(i);
                if (m.tier != tier) continue;
                wideStock = wideStock.add(BigInteger.valueOf(m.stock));
                wideCapacity = wideCapacity.add(BigInteger.valueOf(m.capacity));
            }
        }
        long want = 0, offer = 0;
        for (int i = 0; i < size; i++) {
            Member<H> m = members.get(i);
            m.give = m.take = 0;
            if (m.tier != tier) continue;

            long target =
                    wide
                            ? wideStock
                                    .multiply(BigInteger.valueOf(m.capacity))
                                    .divide(wideCapacity)
                                    .longValue()
                            : Distribution.weightedShare(
                                    sumStock, m.capacity, sumCapacity, Long.MAX_VALUE);
            if (m.stock > target) m.give = Math.min(m.out, m.stock - target);
            else m.take = Math.min(m.in, target - m.stock);
            want = saturatedAdd(want, m.take);
            offer = saturatedAdd(offer, m.give);
        }
        return transfer(port, Math.min(Math.min(want, offer), cap), want, offer);
    }

    private long transfer(Port<H> port, long amount, long want, long offer) {
        if (amount <= 0) return 0;
        long accepted = 0;
        long remaining = amount, remainingWeight = want;
        for (int i = 0; i < size && remaining > 0; i++) {
            Member<H> m = members.get(i);
            long w = m.take;
            if (w <= 0) continue;
            long share =
                    w >= remainingWeight
                            ? Math.min(remaining, w)
                            : Distribution.weightedShare(remaining, w, remainingWeight, w);
            remainingWeight -= w;
            if (share <= 0) continue;
            long took = Math.max(0, Math.min(share, share - port.credit(m.handler, share)));
            m.stock += took;
            m.in -= took;
            accepted += took;
            remaining -= share;
        }
        remaining = accepted;
        remainingWeight = offer;
        for (int i = 0; i < size && remaining > 0; i++) {
            Member<H> m = members.get(i);
            long w = m.give;
            if (w <= 0) continue;
            long share =
                    w >= remainingWeight
                            ? Math.min(remaining, w)
                            : Distribution.weightedShare(remaining, w, remainingWeight, w);
            remainingWeight -= w;
            if (share <= 0) continue;
            port.debit(m.handler, share);
            m.stock -= share;
            m.out -= share;
            remaining -= share;
        }
        return accepted;
    }

    private static long saturatedAdd(long a, long b) {
        long r = a + b;
        return r < 0 ? Long.MAX_VALUE : r;
    }
}
