// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

final class UniformExchangeScalar implements UniformExchange {

    @Override
    public long exchange(double[] uniform, int offA, int offB, int n, double uuE) {
        long changed = 0L;
        for (int i = 0; i < n; i++) {
            if (RadiationSystemNT.exchangeUniExactXZ(uniform, offA + i, offB + i, uuE))
                changed |= 1L << i;
        }
        return changed;
    }
}
