// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.platform.IEnergyHandlerView;
import com.hbm.uninos.graph.Distribution;
import java.util.ArrayList;
import java.util.List;

final class ForeignEnergyEndpoint implements IEnergyHandlerMK2 {

    static final boolean BIDIRECTIONAL_IS_RECEIVER_ONLY = true;

    static final IEnergyHandlerMK2.ConnectionPriority FOREIGN_PRIORITY =
            IEnergyHandlerMK2.ConnectionPriority.LOWEST;
    private final IEnergyHandlerView view;

    private final EnergyConversion.Carry carry = new EnergyConversion.Carry();
    private long availableHe;
    private long roomHe;
    private boolean provider;
    private boolean receiver;

    private boolean bidirectionalSeen;

    private long owedFe;

    ForeignEnergyEndpoint(IEnergyHandlerView view) {
        this.view = view;
    }

    static long fileForeignDemand(
            List<Distribution.Share<IEnergyHandlerMK2>> foreign,
            long powerAvailable,
            double reserve,
            long nativeDemand,
            List<Distribution.Share<IEnergyHandlerMK2>>[] receivers,
            long[] demand) {
        long reserveBudget = reserve > 0 ? (long) (powerAvailable * Math.min(reserve, 1.0)) : 0;
        long surplusBudget = Math.max(0L, powerAvailable - nativeDemand);
        int lowest = FOREIGN_PRIORITY.ordinal();
        int normal = ConnectionPriority.NORMAL.ordinal();
        long added = 0;
        for (Distribution.Share<IEnergyHandlerMK2> share : foreign) {
            long atNormal = Math.min(share.amount(), reserveBudget);
            reserveBudget -= atNormal;
            long atLowest = Math.min(share.amount() - atNormal, surplusBudget);
            surplusBudget -= atLowest;
            if (atNormal > 0) {
                if (receivers[normal] == null) receivers[normal] = new ArrayList<>();
                receivers[normal].add(new Distribution.Share<>(share.endpoint(), atNormal));
                demand[normal] += atNormal;
                added += atNormal;
            }
            if (atLowest > 0) {
                if (receivers[lowest] == null) receivers[lowest] = new ArrayList<>();
                receivers[lowest].add(new Distribution.Share<>(share.endpoint(), atLowest));
                demand[lowest] += atLowest;
                added += atLowest;
            }
        }
        return added;
    }

    void refresh() {
        long insertableFe = view.insertable();
        long extractableFe = view.extractable();
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
            long fits = Math.min(EnergyConversion.feFromHe(power), view.insertable());
            return power - EnergyConversion.heFromFe(fits);
        }
        long fe = carry.feFromHe(power);
        long acceptedFe = fe > 0 ? view.insert(fe, 1L, false) : 0;
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
        owedFe = fe - view.extract(fe, 1L, false);
    }

    EnergyConversion.Carry carry() {
        return carry;
    }
}
