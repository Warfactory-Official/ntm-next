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

package com.hbm.uninos.graph;

import com.hbm.uninos.graph.Distribution.IdentitySet;
import com.hbm.uninos.graph.Distribution.Total;
import java.util.ArrayList;

public final class BufferPool<H> {
    public interface Port<H> {
        long stock(H handler);

        long headroom(H handler);

        long credit(H handler, long amount);

        void debit(H handler, long amount);

        default long outputAllowance(H handler, long speed, long netMoved) {
            return Math.max(0, speed - netMoved);
        }

        default long inputAllowance(H handler, long speed, long netMoved) {
            return Math.max(0, speed - netMoved);
        }

        default boolean reservesSources() {
            return false;
        }

        default void reserve(H handler, long amount) {}

        default void release(H handler, long amount) {}
    }

    @FunctionalInterface
    public interface Observer<H> {
        void accept(H handler, int tier, long output, long input);
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
        long reserved;
    }

    private final ArrayList<Member<H>> members = new ArrayList<>();
    private final IdentitySet<H> seen = new IdentitySet<>();
    private final Total want = new Total();
    private final Total offer = new Total();
    private final Total budget = new Total();
    private final Total remaining = new Total();
    private final Total weight = new Total();
    private final Total accepted = new Total();
    private final Total sumStock = new Total();
    private final Total sumCapacity = new Total();
    private int size;

    public void reset() {
        for (int i = 0; i < size; i++) members.get(i).handler = null;
        size = 0;
        seen.clear();
    }

    public void visit(Observer<H> observer) {
        for (int i = 0; i < size; i++) {
            Member<H> m = members.get(i);
            observer.accept(m.handler, m.tier, m.outSpeed, m.inSpeed);
        }
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

    public void exchange(Port<H> port, int tiers, Total moved) {
        if (size < 2) return;
        int present = 0;
        int takers = 0;
        int givers = 0;
        for (int i = 0; i < size; i++) {
            Member<H> m = members.get(i);
            present |= 1 << m.tier;
            long stock = Math.max(0, port.stock(m.handler));
            long headroom = Math.max(0, port.headroom(m.handler));
            m.stock = stock;
            m.capacity = Math.addExact(stock, headroom);
            m.out =
                    Math.min(
                            stock,
                            port.outputAllowance(
                                    m.handler, m.outSpeed, Math.max(0, m.gathered - stock)));
            m.in =
                    Math.min(
                            headroom,
                            port.inputAllowance(
                                    m.handler, m.inSpeed, Math.max(0, stock - m.gathered)));
            if (m.in > 0) takers |= 1 << m.tier;
            if (m.out > 0) givers |= 1 << m.tier;
        }
        for (int high = tiers - 1; high > 0; high--) {
            if ((takers & 1 << high) == 0) continue;
            for (int low = 0; low < high; low++) {
                if ((givers & 1 << low) == 0) continue;
                want.set(0);
                offer.set(0);
                for (int i = 0; i < size; i++) {
                    Member<H> m = members.get(i);
                    m.take = m.tier == high ? m.in : 0;
                    m.give = m.tier == low ? m.out : 0;
                    want.add(m.take);
                    offer.add(m.give);
                }
                transfer(port, moved);
            }
        }
        for (int t = 0; t < tiers; t++) {
            if ((present & 1 << t) != 0) level(port, t, moved);
        }
    }

    private void level(Port<H> port, int tier, Total moved) {
        sumStock.set(0);
        sumCapacity.set(0);
        int count = 0;
        for (int i = 0; i < size; i++) {
            Member<H> m = members.get(i);
            if (m.tier != tier) continue;
            count++;
            sumStock.add(m.stock);
            sumCapacity.add(m.capacity);
        }
        if (count < 2) return;
        want.set(0);
        offer.set(0);
        for (int i = 0; i < size; i++) {
            Member<H> m = members.get(i);
            m.give = m.take = 0;
            if (m.tier != tier) continue;
            long target = Distribution.weightedShare(sumStock, m.capacity, sumCapacity, m.capacity);
            if (m.stock > target) m.give = Math.min(m.out, m.stock - target);
            else m.take = Math.min(m.in, target - m.stock);
            want.add(m.take);
            offer.add(m.give);
        }
        transfer(port, moved);
    }

    private void transfer(Port<H> port, Total moved) {
        budget.setMin(want, offer);
        if (budget.isZero()) return;
        boolean reserve = port.reservesSources();
        if (reserve) {
            remaining.set(budget);
            weight.set(offer);
            for (int i = 0; i < size; i++) {
                Member<H> m = members.get(i);
                long share =
                        Distribution.weightedShare(
                                remaining, m.give, weight, Math.min(m.give, remaining.limit()));
                weight.subtract(m.give);
                m.reserved = 0;
                if (share > 0) {
                    port.reserve(m.handler, share);
                    m.reserved = share;
                }
                remaining.subtract(share);
            }
        }
        accepted.set(0);
        remaining.set(budget);
        weight.set(want);
        for (int i = 0; i < size && !remaining.isZero(); i++) {
            Member<H> m = members.get(i);
            long w = m.take;
            if (w <= 0) continue;
            long share =
                    Distribution.weightedShare(
                            remaining, w, weight, Math.min(w, remaining.limit()));
            weight.subtract(w);
            if (share <= 0) continue;
            long took = Math.max(0, Math.min(share, share - port.credit(m.handler, share)));
            m.stock += took;
            m.in -= took;
            accepted.add(took);
            remaining.subtract(share);
        }
        remaining.set(accepted);
        weight.set(offer);
        for (int i = 0; i < size && !remaining.isZero(); i++) {
            Member<H> m = members.get(i);
            long w = m.give;
            if (w <= 0) continue;
            long share =
                    Distribution.weightedShare(
                            remaining, w, weight, Math.min(w, remaining.limit()));
            weight.subtract(w);
            if (share <= 0) continue;
            port.debit(m.handler, share);
            m.stock -= share;
            m.out -= share;
            remaining.subtract(share);
        }
        moved.add(accepted);
        if (reserve) {
            for (int i = 0; i < size; i++) {
                Member<H> m = members.get(i);
                if (m.reserved > 0) {
                    port.release(m.handler, m.reserved);
                    m.reserved = 0;
                }
            }
        }
    }
}
