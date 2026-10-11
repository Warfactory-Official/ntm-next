// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.platform.FabricTrEnergyBridge;
import com.hbm.uninos.graph.Distribution.Bucket;
import com.hbm.uninos.graph.Distribution.Total;
import team.reborn.energy.api.EnergyStorage;

final class ForeignEnergyEndpoint implements IEnergyHandlerMK2 {
    static final boolean BIDIRECTIONAL_IS_RECEIVER_ONLY = true;

    static final IEnergyHandlerMK2.ConnectionPriority FOREIGN_PRIORITY =
            IEnergyHandlerMK2.ConnectionPriority.LOWEST;
    EnergyStorage store;
    private final EnergyConversion.Carry carry = new EnergyConversion.Carry();
    private long availableHe;
    private long roomHe;
    private boolean provider;
    private boolean receiver;

    private boolean bidirectionalSeen;
    private long owedFe;

    ForeignEnergyEndpoint(EnergyStorage store) {
        this.store = store;
    }

    static void fileForeignDemand(
            Bucket<IEnergyHandlerMK2> foreign,
            Total powerAvailable,
            double reserve,
            Total nativeDemand,
            Bucket<IEnergyHandlerMK2>[] receivers,
            Total reserveBudget,
            Total surplusBudget,
            Total added) {
        reserveBudget.fraction(powerAvailable, Math.clamp(reserve, 0.0, 1.0));
        surplusBudget.positiveDifference(powerAvailable, nativeDemand);
        int lowest = FOREIGN_PRIORITY.ordinal();
        int normal = ConnectionPriority.NORMAL.ordinal();
        added.set(0);
        for (int i = 0; i < foreign.size(); i++) {
            long amount = foreign.amount(i);
            long atNormal = Math.min(amount, reserveBudget.limit());
            reserveBudget.subtract(atNormal);
            long atLowest = Math.min(amount - atNormal, surplusBudget.limit());
            surplusBudget.subtract(atLowest);
            if (atNormal > 0) {
                if (receivers[normal] == null) receivers[normal] = new Bucket<>();
                receivers[normal].add(foreign.endpoint(i), atNormal);
                added.add(atNormal);
            }
            if (atLowest > 0) {
                if (receivers[lowest] == null) receivers[lowest] = new Bucket<>();
                receivers[lowest].add(foreign.endpoint(i), atLowest);
                added.add(atLowest);
            }
        }
    }

    void refresh() {
        long insertableFe = FabricTrEnergyBridge.insertable(store);
        long extractableFe = FabricTrEnergyBridge.extractable(store);
        boolean canInsert = insertableFe > 0;
        boolean canExtract = extractableFe > 0;
        if (canInsert && canExtract) bidirectionalSeen = true;

        receiver = canInsert;
        provider = canExtract && !(BIDIRECTIONAL_IS_RECEIVER_ONLY && bidirectionalSeen);

        availableHe = provider ? EnergyConversion.heFromFe(Math.max(0, extractableFe - owedFe)) : 0;
        roomHe = receiver ? EnergyConversion.heFromFe(insertableFe) : 0;
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
    public long getProviderSpeed() {
        return availableHe;
    }

    @Override
    public long getReceiverSpeed() {
        return roomHe;
    }

    @Override
    public ConnectionPriority getPriority() {
        return FOREIGN_PRIORITY;
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (power <= 0 || !receiver) return power;
        if (simulate) {
            long fits =
                    Math.min(
                            EnergyConversion.feFromHe(power),
                            FabricTrEnergyBridge.insertable(store));
            return power - EnergyConversion.heFromFe(fits);
        }
        long fe = carry.feFromHe(power);
        long acceptedFe = fe > 0 ? FabricTrEnergyBridge.transfer(store, fe, 1L, false, true) : 0;
        long unplacedHe = acceptedFe < fe ? carry.refundFe(fe - acceptedFe, power) : 0;
        roomHe = Math.max(0, roomHe - (power - unplacedHe));
        return unplacedHe;
    }

    @Override
    public void usePower(long power) {
        if (power <= 0) return;
        availableHe = Math.max(0, availableHe - power);
        long fe = carry.feFromHe(power) + owedFe;
        if (fe <= 0) return;
        owedFe = fe - FabricTrEnergyBridge.transfer(store, fe, 1L, false, false);
    }

    EnergyConversion.Carry carry() {
        return carry;
    }
}
