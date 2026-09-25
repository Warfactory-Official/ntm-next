// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class NeoForgeEnergyHandlerView implements IEnergyHandlerView {

    private final EnergyHandler handler;

    NeoForgeEnergyHandlerView(EnergyHandler handler) {
        this.handler = handler;
    }

    @Override
    public Object handlerIdentity() {
        return handler;
    }

    @Override
    public long amount() {
        return handler.getAmountAsLong();
    }

    @Override
    public long capacity() {
        return handler.getCapacityAsLong();
    }

    @Override
    public long insertable() {
        try (Transaction tx = Transaction.openRoot()) {
            return handler.insert(Integer.MAX_VALUE, tx);
        }
    }

    @Override
    public long extractable() {
        try (Transaction tx = Transaction.openRoot()) {
            return handler.extract(Integer.MAX_VALUE, tx);
        }
    }

    @Override
    public long insert(long maxFe, long feQuantum, boolean simulate) {
        return transfer(maxFe, feQuantum, simulate, true);
    }

    @Override
    public long extract(long maxFe, long feQuantum, boolean simulate) {
        return transfer(maxFe, feQuantum, simulate, false);
    }

    private long transfer(long maxFe, long feQuantum, boolean simulate, boolean insert) {
        assert feQuantum > 0;
        long request = Math.min(maxFe, Integer.MAX_VALUE);
        request -= request % feQuantum;
        if (request <= 0) return 0;

        try (Transaction outer = Transaction.openRoot()) {
            long available;
            try (Transaction probe = Transaction.open(outer)) {
                available =
                        insert
                                ? handler.insert((int) request, probe)
                                : handler.extract((int) request, probe);
            }
            long exact = available - available % feQuantum;
            if (exact <= 0) return 0;

            long moved =
                    insert
                            ? handler.insert((int) exact, outer)
                            : handler.extract((int) exact, outer);
            if (moved != exact) return 0;
            if (!simulate) outer.commit();
            return exact;
        }
    }
}
