// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.lib.internal.VectorApi;
import com.hbm.lib.internal.VectorBackend;
import org.slf4j.LoggerFactory;

public interface UniformExchange {

    UniformExchange INSTANCE = choose();

    long exchange(double[] uniform, int offA, int offB, int n, double uuE);

    private static UniformExchange choose() {
        VectorBackend backend = VectorApi.backend();

        if (backend.arithmetic() && backend.masks() && backend.fma()) {
            try {
                return new UniformExchangeVector();
            } catch (Throwable t) {
                LoggerFactory.getLogger("NTM")
                        .warn("Vector uniform exchange unavailable; using scalar", t);
            }
        }
        return new UniformExchangeScalar();
    }
}
