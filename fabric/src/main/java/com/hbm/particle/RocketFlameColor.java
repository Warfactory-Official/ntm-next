// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

public record RocketFlameColor(
        float r, float g, float b, boolean fadeR, boolean fadeG, boolean fadeB, int light) {
    public static final RocketFlameColor FLAME =
            new RocketFlameColor(1F, 0.6F, 0F, true, true, true, 240);

    public static RocketFlameColor tinted(float r, float g, float b) {
        return new RocketFlameColor(
                r != 0F ? r : FLAME.r,
                g != 0F ? g : FLAME.g,
                b != 0F ? b : FLAME.b,
                true,
                true,
                true,
                FLAME.light);
    }

    public float red(float fade, float add) {
        return (fadeR ? r * fade : r) + add;
    }

    public float green(float fade, float add) {
        return (fadeG ? g * fade : g) + add;
    }

    public float blue(float fade, float add) {
        return (fadeB ? b * fade : b) + add;
    }
}
