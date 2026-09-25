// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.lib.internal.VectorApi;
import com.hbm.lib.internal.VectorBackend;
import org.slf4j.LoggerFactory;

public interface UniformDecay {

    int CHANGED = 0;
    int ZEROED = 1;
    int FOG = 2;
    int DESTROY = 3;

    UniformDecay INSTANCE = choose();

    void decay(
            double[] uniform,
            int off,
            int n,
            double retention,
            double minBound,
            double fogRad,
            double destroyRad,
            long[] out);

    private static UniformDecay choose() {
        VectorBackend backend = VectorApi.backend();

        if (backend.arithmetic() && backend.masks()) {
            try {
                return new UniformDecayVector();
            } catch (Throwable t) {
                LoggerFactory.getLogger("NTM")
                        .warn("Vector uniform decay unavailable; using scalar", t);
            }
        }
        return new UniformDecayScalar();
    }
}
