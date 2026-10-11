// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

final class NoiseBand {

    private NoiseBand() {}

    static int halfHeight(double n, double threshold, int rangeMult, int maxRange) {
        if (n <= threshold) return -1;
        int range = (int) ((n - threshold) * rangeMult);
        return range > maxRange ? maxRange * 2 - range : range;
    }
}
