// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.util.Mth;

public final class RotatedQuad {

    private RotatedQuad() {}

    public static float leftOf(float a, float b, float cos, float sin) {
        return a * cos + b * sin;
    }

    public static float upOf(float a, float b, float cos, float sin) {
        return b * cos - a * sin;
    }

    public static float cos(float degrees) {
        return Mth.cos(degrees * Mth.DEG_TO_RAD);
    }

    public static float sin(float degrees) {
        return Mth.sin(degrees * Mth.DEG_TO_RAD);
    }
}
