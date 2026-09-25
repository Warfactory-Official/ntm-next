// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.uninos.graph.Distribution;
import java.util.List;

public final class EnergyBus {

    private EnergyBus() {}

    public static long serveShortage(
            List<Distribution.Share<IEnergyHandlerMK2>>[] byPriority,
            long[] demand,
            long budget,
            Distribution.Apply<IEnergyHandlerMK2> apply) {
        long delivered = 0;
        long remaining = budget;

        for (int t = byPriority.length - 1; t >= 0 && remaining > 0; t--) {
            List<Distribution.Share<IEnergyHandlerMK2>> tier = byPriority[t];
            if (tier == null || tier.isEmpty()) continue;
            long remainingDemand = demand[t];
            if (remainingDemand <= 0) continue;
            long tierBudget = remaining;

            for (int i = 0, size = tier.size(); i < size && tierBudget > 0; i++) {
                Distribution.Share<IEnergyHandlerMK2> row = tier.get(i);
                long want = row.amount();

                long offer =
                        Distribution.weightedShare(
                                tierBudget, want, remainingDemand, Math.min(want, tierBudget));
                remainingDemand -= want;
                if (offer <= 0) continue;
                long taken = Math.min(apply.give(row.endpoint(), offer), offer);
                if (taken <= 0) continue;
                tierBudget -= taken;
                delivered += taken;
            }

            remaining = tierBudget;
        }
        return delivered;
    }

    public static long serveInFull(
            List<Distribution.Share<IEnergyHandlerMK2>>[] byPriority,
            long budget,
            Distribution.Apply<IEnergyHandlerMK2> apply) {
        long delivered = 0;
        long remaining = budget;
        for (int t = byPriority.length - 1; t >= 0; t--) {
            List<Distribution.Share<IEnergyHandlerMK2>> tier = byPriority[t];
            if (tier == null || tier.isEmpty()) continue;
            for (int i = 0, size = tier.size(); i < size; i++) {
                Distribution.Share<IEnergyHandlerMK2> row = tier.get(i);
                if (remaining <= 0) return delivered;
                long give = Math.min(row.amount(), remaining);
                if (give <= 0) continue;
                long taken = Math.min(apply.give(row.endpoint(), give), give);
                if (taken <= 0) continue;
                delivered += taken;
                remaining -= taken;
            }
        }
        return delivered;
    }
}
