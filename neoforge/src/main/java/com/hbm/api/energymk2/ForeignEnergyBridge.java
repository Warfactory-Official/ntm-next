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

package com.hbm.api.energymk2;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.math.BigInteger;
import org.jspecify.annotations.Nullable;

public final class ForeignEnergyBridge {
    public static final int RECEIVE_LATCH_TICKS = 4;
    private final Long2LongOpenHashMap latchByNeighbour = new Long2LongOpenHashMap();
    private final EnergyHandler injectedSource =
            new EnergyHandler() {

                @Override
                public long getPower() {
                    return injectedHe;
                }

                @Override
                public void setPower(long power) {
                    injectedHe = Math.max(0, power);
                    changed();
                }

                @Override
                public long getMaxPower() {
                    return injectedHe;
                }

                @Override
                public long getSendRate() {
                    return injectedHe;
                }
            };
    private final EnergyConversion.Carry carry = new EnergyConversion.Carry();
    private long injectedHe;
    private long insertBudgetHe;
    private long extractBudgetHe;
    private long owedHe;
    private long relayReserveHe;
    private boolean distributing;
    public @Nullable Object loaderStaging;
    @Nullable PowerTransitLedger ledger;
    @Nullable MixedEnergyPlan plan;
    int planIndex = -1;

    public ForeignEnergyBridge() {
        latchByNeighbour.defaultReturnValue(Long.MIN_VALUE);
    }

    void changed() {
        if (ledger != null) ledger.setDirty();
    }

    void restoreConserved(long injected, long owed, long heToFe, long feToHe) {
        injectedHe = Math.addExact(injectedHe, injected);
        owedHe = Math.addExact(owedHe, owed);
        EnergyConversion.Carry restored = new EnergyConversion.Carry();
        restored.restore(heToFe, feToHe);
        carry.absorb(restored);
    }

    public State snapshot() {
        return new State(
                injectedHe,
                insertBudgetHe,
                extractBudgetHe,
                owedHe,
                carry.heToFeRemainder(),
                carry.feToHeRemainder(),
                new Long2LongOpenHashMap(latchByNeighbour));
    }

    public void restore(State state) {
        injectedHe = state.injected();
        insertBudgetHe = state.insertBudget();
        extractBudgetHe = state.extractBudget();
        owedHe = state.owed();
        carry.restore(state.heToFeCarry(), state.feToHeCarry());
        latchByNeighbour.clear();
        latchByNeighbour.putAll(state.latch());
        changed();
    }

    public @Nullable MixedEnergyPlan plan() {
        MixedEnergyPlan current = plan;
        return current != null && current.active ? current : null;
    }

    void bind(MixedEnergyPlan plan, int index) {
        this.plan = plan;
        planIndex = index;
    }

    void unbind() {
        plan = null;
        planIndex = -1;
    }

    public long insertableFe() {
        return distributing
                ? 0
                : EnergyConversion.feFromHe(Math.min(insertBudgetHe, Long.MAX_VALUE - injectedHe));
    }

    public long extractableFe() {
        return distributing ? 0 : EnergyConversion.feFromHe(extractBudgetHe);
    }

    public long plannedInsertableFe() {
        return distributing ? 0 : EnergyConversion.feFromHe(Long.MAX_VALUE - injectedHe);
    }

    public long plannedExtractableFe() {
        return distributing ? 0 : paidFe(relayReserveHe);
    }

    public void insertPlannedFe(long fe) {
        if (fe <= 0 || distributing) return;
        fundFe(fe);
    }

    public long extractPlannedFe(long fe) {
        long given = Math.min(fe, plannedExtractableFe());
        if (given > 0) spendFe(given);
        return given;
    }

    void normalize() {
        long numerator = Math.addExact(carry.heToFeRemainder(), carry.feToHeRemainder());
        long divisor = EnergyConversion.feRatio();
        long he = numerator / divisor;
        if (he == 0 && carry.heToFeRemainder() == 0) return;
        injectedHe = Math.addExact(injectedHe, he);
        carry.restore(0, numerator % divisor);
        changed();
    }

    long paidFe(long reserveHe) {
        if (injectedHe < reserveHe) return 0;
        long he = injectedHe - reserveHe;
        long remainder = Math.addExact(carry.heToFeRemainder(), carry.feToHeRemainder());
        long multiplier = EnergyConversion.feRatio();
        long divisor = EnergyConversion.heRatio();
        if (he <= (Long.MAX_VALUE - remainder) / multiplier)
            return (he * multiplier + remainder) / divisor;
        return BigInteger.valueOf(he)
                .multiply(BigInteger.valueOf(multiplier))
                .add(BigInteger.valueOf(remainder))
                .divide(BigInteger.valueOf(divisor))
                .min(BigInteger.valueOf(Long.MAX_VALUE))
                .longValueExact();
    }

    void spendFe(long fe) {
        normalize();
        long[] remainder = {0};
        long he =
                EnergyConversion.convert(
                        fe, EnergyConversion.heRatio(), EnergyConversion.feRatio(), remainder);
        long residual = carry.feToHeRemainder();
        if (residual < remainder[0]) {
            he++;
            residual += EnergyConversion.feRatio();
        }
        injectedHe -= he;
        carry.restore(0, residual - remainder[0]);
        changed();
    }

    void fundFe(long fe) {
        if (fe <= 0) return;
        injectedHe = Math.addExact(injectedHe, carry.heFromFe(fe));
        changed();
    }

    void afterRound(long unmetNativeHe) {
        relayReserveHe = unmetNativeHe;
    }

    EnergyConversion.Carry transferCarry() {
        return carry;
    }

    public long owed() {
        return owedHe;
    }

    public long insertFe(long fe, long neighbourKey, long gameTime) {
        if (fe <= 0 || distributing) return 0;
        long accepted = Math.min(fe, insertableFe());
        if (accepted <= 0) return 0;
        long he = carry.heFromFe(accepted);
        if (he > insertBudgetHe) he = insertBudgetHe;
        insertBudgetHe -= he;
        injectedHe += he;
        latchByNeighbour.put(neighbourKey, gameTime);
        changed();
        return accepted;
    }

    public long extractFe(long fe) {
        if (fe <= 0 || distributing) return 0;
        long given = Math.min(fe, EnergyConversion.feFromHe(extractBudgetHe));
        if (given <= 0) return 0;
        long he = carry.heFromFe(given);
        if (he > extractBudgetHe) he = extractBudgetHe;
        extractBudgetHe -= he;
        owedHe += he;
        changed();
        return given;
    }

    public long amountFe() {
        return EnergyConversion.feFromHe(extractBudgetHe);
    }

    public long capacityFe() {
        long total =
                amountFe()
                        + EnergyConversion.feFromHe(
                                Math.min(insertBudgetHe, Long.MAX_VALUE - injectedHe));
        return total < 0 ? Long.MAX_VALUE : total;
    }

    public void absorb(ForeignEnergyBridge other) {
        injectedHe = Math.addExact(injectedHe, other.injectedHe);
        owedHe = Math.addExact(owedHe, other.owedHe);
        carry.absorb(other.carry);
        for (ObjectIterator<Long2LongMap.Entry> it =
                        other.latchByNeighbour.long2LongEntrySet().fastIterator();
                it.hasNext(); ) {
            Long2LongMap.Entry e = it.next();
            long mine = latchByNeighbour.get(e.getLongKey());
            if (mine == Long.MIN_VALUE || e.getLongValue() > mine) {
                latchByNeighbour.put(e.getLongKey(), e.getLongValue());
            }
        }
        other.injectedHe = 0;
        other.owedHe = 0;
        other.carry.restore(0, 0);
        other.unbind();
        if (other.ledger != null) other.ledger.retire(other);
        changed();
    }

    public boolean beginDistribute() {
        if (distributing) return false;
        distributing = true;
        return true;
    }

    public void endDistribute() {
        distributing = false;
    }

    public long injected() {
        return injectedHe;
    }

    public EnergyHandler injectedSource() {
        return injectedSource;
    }

    public long drainOwed() {
        long owed = owedHe;
        owedHe = 0;
        if (owed != 0) changed();
        return owed;
    }

    public void carryOwed(long he) {
        if (he > 0) {
            owedHe += he;
            changed();
        }
    }

    public boolean latched(long neighbourKey, long gameTime) {
        long stamp = latchByNeighbour.get(neighbourKey);
        return stamp != Long.MIN_VALUE && gameTime - stamp <= RECEIVE_LATCH_TICKS;
    }

    public void publish(long surplusHe, long unmetNativeHe, long gameTime) {
        extractBudgetHe = Math.max(0, surplusHe);
        insertBudgetHe = Math.max(0, unmetNativeHe);
        if (latchByNeighbour.isEmpty()) return;
        for (ObjectIterator<Long2LongMap.Entry> it =
                        latchByNeighbour.long2LongEntrySet().fastIterator();
                it.hasNext(); ) {
            if (gameTime - it.next().getLongValue() > RECEIVE_LATCH_TICKS) it.remove();
        }
    }

    public record State(
            long injected,
            long insertBudget,
            long extractBudget,
            long owed,
            long heToFeCarry,
            long feToHeCarry,
            Long2LongMap latch) {}
}
