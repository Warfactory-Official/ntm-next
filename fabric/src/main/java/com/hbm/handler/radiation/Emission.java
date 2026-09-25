// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

final class Emission {

    private Emission() {}

    static boolean active(double emission, double saturation, double c) {
        if (emission == 0.0D || saturation == 0.0D) return false;
        return emission * (saturation - c) > 0.0D;
    }

    static double weight(double emission, double saturation) {
        return magnitude(emission) / magnitude(saturation);
    }

    static double numeratorTerm(double emission, double saturation) {
        return saturation < 0.0D ? -magnitude(emission) : magnitude(emission);
    }

    static double relax(double c, double weightSum, double numerator) {
        if (weightSum <= 0.0D) return c;

        if (weightSum > 1.0D) return numerator / weightSum;
        return c + (numerator - c * weightSum);
    }

    static double single(double c, double emission, double saturation) {
        if (!active(emission, saturation, c)) return c;
        return relax(c, weight(emission, saturation), numeratorTerm(emission, saturation));
    }

    private static double magnitude(double v) {
        return v < 0.0D ? -v : v;
    }
}
