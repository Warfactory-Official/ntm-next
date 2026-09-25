// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public final class ForeignEnergyBridge {

    public static final int RECEIVE_LATCH_TICKS = 4;

    private final Long2LongOpenHashMap latchByNeighbour = new Long2LongOpenHashMap();

    private final IEnergyHandlerMK2 injectedSource =
            new IEnergyHandlerMK2() {

                @Override
                public long getPower() {
                    return injectedHe;
                }

                @Override
                public void setPower(long power) {
                    injectedHe = Math.max(0, power);
                }

                @Override
                public long getMaxPower() {
                    return injectedHe;
                }

                @Override
                public long getProviderSpeed() {
                    return injectedHe;
                }
            };

    private final EnergyConversion.Carry carry = new EnergyConversion.Carry();
    private long injectedHe;
    private long insertBudgetHe;
    private long extractBudgetHe;
    private long owedHe;

    private boolean distributing;

    public ForeignEnergyBridge() {
        latchByNeighbour.defaultReturnValue(Long.MIN_VALUE);
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
    }

    public long insertableFe() {
        return distributing ? 0 : EnergyConversion.feFromHe(insertBudgetHe);
    }

    public long extractableFe() {
        return distributing ? 0 : EnergyConversion.feFromHe(extractBudgetHe);
    }

    public long insertFe(long fe, long neighbourKey, long gameTime) {
        if (fe <= 0 || distributing) return 0;
        long accepted = Math.min(fe, EnergyConversion.feFromHe(insertBudgetHe));
        if (accepted <= 0) return 0;
        long he = carry.heFromFe(accepted);
        if (he > insertBudgetHe) he = insertBudgetHe;
        insertBudgetHe -= he;
        injectedHe += he;
        latchByNeighbour.put(neighbourKey, gameTime);
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
        return given;
    }

    public long amountFe() {
        return EnergyConversion.feFromHe(extractBudgetHe);
    }

    public long capacityFe() {
        long total = amountFe() + EnergyConversion.feFromHe(insertBudgetHe);
        return total < 0 ? Long.MAX_VALUE : total;
    }

    public void absorb(ForeignEnergyBridge other) {
        injectedHe += other.injectedHe;
        owedHe += other.owedHe;
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

    public IEnergyHandlerMK2 injectedSource() {
        return injectedSource;
    }

    public long drainOwed() {
        long owed = owedHe;
        owedHe = 0;
        return owed;
    }

    public void carryOwed(long he) {
        if (he > 0) owedHe += he;
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
