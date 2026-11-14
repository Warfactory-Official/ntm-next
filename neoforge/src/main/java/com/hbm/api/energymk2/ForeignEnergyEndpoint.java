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

import com.hbm.platform.Caps;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.uninos.graph.Distribution.Bucket;
import com.hbm.uninos.graph.Distribution.Total;
import com.hbm.uninos.graph.Distribution;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

final class ForeignEnergyEndpoint implements EnergyHandler, EnergyProbe {
    static final boolean BIDIRECTIONAL_IS_RECEIVER_ONLY = true;

    static final ConnectionPriority FOREIGN_PRIORITY = ConnectionPriority.LOWEST;
    net.neoforged.neoforge.transfer.energy.EnergyHandler store;
    private final EnergyConversion.Carry carry = new EnergyConversion.Carry();
    ServerLevel level;
    long posKey;
    Direction side;
    @Nullable ForeignEnergyNode node;
    long probedInput;
    long probedOutput;
    private int directions;
    private long availableHe;
    private long roomHe;
    private boolean provider;
    private boolean receiver;
    private long owedFe;
    private @Nullable ForeignEnergyBridge bank;
    private @Nullable MixedEnergyPlan plan;
    private int toSlot = -1;
    private int fromSlot = -1;

    ForeignEnergyEndpoint(net.neoforged.neoforge.transfer.energy.EnergyHandler store) {
        this.store = store;
    }

    static <E> void fileForeignDemand(
            Bucket<E> foreign,
            Total powerAvailable,
            double reserve,
            Total consumerDemand,
            Bucket<E>[] receivers,
            Total reserveBudget,
            Total foreignBudget,
            Total added) {
        reserveBudget.fraction(powerAvailable, Math.clamp(reserve, 0.0, 1.0));
        foreignBudget.positiveDifference(powerAvailable, consumerDemand);
        if (foreignBudget.compareTo(reserveBudget) < 0) foreignBudget.set(reserveBudget);
        foreignBudget.setMin(foreignBudget, foreign.total);
        reserveBudget.setMin(reserveBudget, foreignBudget);
        Total weight = foreign.weight.set(foreign.total);
        int lowest = FOREIGN_PRIORITY.ordinal();
        int normal = ConnectionPriority.NORMAL.ordinal();
        added.set(foreignBudget);
        for (int i = 0; i < foreign.size(); i++) {
            long want = foreign.amount(i);
            long amount = Distribution.weightedShare(foreignBudget, want, weight, want);
            long atNormal =
                    Distribution.weightedShare(reserveBudget, amount, foreignBudget, amount);
            weight.subtract(want);
            foreignBudget.subtract(amount);
            reserveBudget.subtract(atNormal);
            long atLowest = amount - atNormal;
            if (atNormal > 0) {
                if (receivers[normal] == null) receivers[normal] = new Bucket<>();
                receivers[normal].add(foreign.endpoint(i), atNormal);
            }
            if (atLowest > 0) {
                if (receivers[lowest] == null) receivers[lowest] = new Bucket<>();
                receivers[lowest].add(foreign.endpoint(i), atLowest);
            }
        }
    }

    void bind(ServerLevel level, long posKey, Direction side, @Nullable ForeignEnergyNode node) {
        this.level = level;
        this.posKey = posKey;
        this.side = side;
        this.node = node;
        plan = null;
        toSlot = fromSlot = -1;
    }

    boolean planned() {
        return node != null && node.kind() != ForeignEnergyNode.Kind.CONSUMER;
    }

    Object identity() {
        return node == null ? store : node.identity();
    }

    void refresh() {
        boolean planned = planned();
        Caps.probeEnergy(store, this);
        receiver = probedInput > 0;
        provider =
                probedOutput > 0
                        && (planned
                                || !BIDIRECTIONAL_IS_RECEIVER_ONLY
                                || directions != (RECEIVE | SEND));
        long output = Math.max(0, probedOutput - owedFe);
        availableHe =
                provider
                        ? planned
                                ? EnergyConversion.heFromFeCeil(output)
                                : EnergyConversion.heFromFe(output)
                        : 0;
        roomHe =
                receiver
                        ? planned
                                ? EnergyConversion.heFromFeCeil(probedInput)
                                : EnergyConversion.heFromFe(probedInput)
                        : 0;
    }

    @Override
    public void energyProbe(long input, long output, int directions) {
        probedInput = input;
        probedOutput = output;
        this.directions = directions;
    }

    void bank(ForeignEnergyBridge bank) {
        if (this.bank != bank) {
            bank.transferCarry().absorb(carry);
            carry.restore(0, 0);
            bank.changed();
        }
        this.bank = bank;
    }

    void plan(MixedEnergyPlan plan, int edge) {
        this.plan = plan;
        toSlot = MixedEnergyPlan.slot(edge, true);
        fromSlot = MixedEnergyPlan.slot(edge, false);
    }

    private long towardFe() {
        return plan == null || !plan.active ? 0 : plan.availableFe(toSlot);
    }

    private long fromFe() {
        return plan == null || !plan.active ? 0 : plan.availableFe(fromSlot);
    }

    private void committed(long fe) {
        if (fe > 0 && level != null) ForeignEnergyAdapters.committed(level, BlockPos.of(posKey));
    }

    long prefund(long maximumHe) {
        maximumHe = Math.min(maximumHe, Long.MAX_VALUE - bank.injected());
        long request =
                Math.min(Math.min(probedOutput, fromFe()), EnergyConversion.feFromHe(maximumHe));
        long fe = request > 0 ? Caps.transfer(store, request, (long) 1, false, false) : 0;
        if (fe <= 0) return 0;
        plan.take(fromSlot, fe);
        bank.fundFe(fe);
        probedOutput -= fe;
        committed(fe);
        return EnergyConversion.heFromFe(fe);
    }

    long relay(long reserveHe) {
        long offered = Math.min(Math.min(probedInput, towardFe()), bank.paidFe(reserveHe));
        long accepted = offered > 0 ? Caps.transfer(store, offered, (long) 1, false, true) : 0;
        if (accepted <= 0) return 0;
        plan.take(toSlot, accepted);
        bank.spendFe(accepted);
        probedInput -= accepted;
        roomHe = EnergyConversion.heFromFeCeil(probedInput);
        committed(accepted);
        return accepted;
    }

    boolean servesAsProvider() {
        return provider && availableHe > 0;
    }

    boolean servesAsReceiver() {
        return receiver && roomHe > 0;
    }

    @Override
    public long getPower() {
        return availableHe;
    }

    @Override
    public void setPower(long power) {
        throw new UnsupportedOperationException(
                "a foreign energy endpoint has no absolute HE level to set");
    }

    @Override
    public long getMaxPower() {
        long total = availableHe + roomHe;
        return total < 0 ? Long.MAX_VALUE : total;
    }

    @Override
    public long getSendRate() {
        return availableHe;
    }

    @Override
    public long getReceiveRate() {
        return roomHe;
    }

    @Override
    public ConnectionPriority getEnergyTier() {
        return FOREIGN_PRIORITY;
    }

    @Override
    public long transferPower(long power) {
        if (power <= 0 || !receiver) return power;
        boolean planned = planned();
        EnergyConversion.Carry transfer = bank != null ? bank.transferCarry() : carry;
        long fe = transfer.feFromHe(power);
        long offered = planned ? Math.min(fe, towardFe()) : fe;
        long acceptedFe = offered > 0 ? Caps.transfer(store, offered, 1L, false, true) : 0;
        long unplacedHe = acceptedFe < fe ? transfer.refundFe(fe - acceptedFe, power) : 0;
        if (planned && acceptedFe > 0) plan.take(toSlot, acceptedFe);
        if (bank != null) bank.changed();
        roomHe = Math.max(0, roomHe - (power - unplacedHe));
        committed(acceptedFe);
        return unplacedHe;
    }

    @Override
    public void usePower(long power) {
        if (power <= 0) return;
        availableHe = Math.max(0, availableHe - power);
        long fe = (bank != null ? bank.transferCarry() : carry).feFromHe(power) + owedFe;
        if (bank != null) bank.changed();
        if (fe <= 0) return;
        long taken = Caps.transfer(store, fe, 1L, false, false);
        owedFe = fe - taken;
        committed(taken);
    }

    EnergyConversion.Carry carry() {
        return carry;
    }
}
