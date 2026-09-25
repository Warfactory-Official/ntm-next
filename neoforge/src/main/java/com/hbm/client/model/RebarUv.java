// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.math.Quadrant;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.core.Direction;

final class RebarUv {

    private RebarUv() {}

    record Uv(CuboidFace.UVs uvs, Quadrant quadrant) {}

    static final RebarUv.Uv[] W0 = {
        new RebarUv.Uv(new CuboidFace.UVs(3F, 5F, 5F, 3F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, 3F, 5F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, -0.016000748F, 13F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, -0.016000748F, 5F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, -0.016000748F, 5F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, -0.016000748F, 13F, 16.016F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W1 = {
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 5F, 16.016F, 3F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 3F, 16.016F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016000748F, 11F, 16.016F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 11F, 16.016F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, 11F, 5F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, 11F, 13F, 13F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W2 = {
        new RebarUv.Uv(new CuboidFace.UVs(3F, 16.016F, 5F, -0.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, -0.016F, 5F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, 11F, 13F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, 11F, 5F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 11F, 16.016F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016000748F, 11F, 16.016F, 13F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W3 = {
        new RebarUv.Uv(new CuboidFace.UVs(3F, 13F, 5F, 11F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, 11F, 5F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, -0.016000748F, 13F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, -0.016000748F, 5F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, -0.016000748F, 13F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, -0.016000748F, 5F, 16.016F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W4 = {
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 13F, 16.016F, 11F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 11F, 16.016F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016000748F, 11F, 16.016F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 11F, 16.016F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, 11F, 13F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, 11F, 5F, 13F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W5 = {
        new RebarUv.Uv(new CuboidFace.UVs(3F, 16.016F, 5F, -0.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, -0.016F, 5F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, 3F, 13F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, 3F, 5F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 3F, 16.016F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016000748F, 3F, 16.016F, 5F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W6 = {
        new RebarUv.Uv(new CuboidFace.UVs(11F, 5F, 13F, 3F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, 3F, 13F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, -0.016000748F, 5F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, -0.016000748F, 13F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, -0.016000748F, 5F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, -0.016000748F, 13F, 16.016F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W7 = {
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 5F, 16.016F, 3F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 3F, 16.016F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016000748F, 3F, 16.016F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 3F, 16.016F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, 3F, 5F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, 3F, 13F, 5F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W8 = {
        new RebarUv.Uv(new CuboidFace.UVs(11F, 16.016F, 13F, -0.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, -0.016F, 13F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, 11F, 5F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, 11F, 13F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 11F, 16.016F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016000748F, 11F, 16.016F, 13F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W9 = {
        new RebarUv.Uv(new CuboidFace.UVs(11F, 13F, 13F, 11F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, 11F, 13F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, -0.016000748F, 5F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, -0.016000748F, 13F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, -0.016000748F, 13F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, -0.016000748F, 5F, 16.016F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W10 = {
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 13F, 16.016F, 11F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 11F, 16.016F, 13F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016000748F, 3F, 16.016F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 3F, 16.016F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, 3F, 13F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, 3F, 5F, 5F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W11 = {
        new RebarUv.Uv(new CuboidFace.UVs(11F, 16.016F, 13F, -0.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, -0.016F, 13F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(3F, 3F, 5F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(11F, 3F, 13F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 3F, 16.016F, 5F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016000748F, 3F, 16.016F, 5F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W12 = {
        new RebarUv.Uv(new CuboidFace.UVs(7F, 9F, 9F, 7F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(7F, 7F, 9F, 9F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(7F, -0.016000748F, 9F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(7F, -0.016000748F, 9F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(7F, -0.016000748F, 9F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(7F, -0.016000748F, 9F, 16.016F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W13 = {
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 9F, 16.016F, 7F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 7F, 16.016F, 9F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016000748F, 7F, 16.016F, 9F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 7F, 16.016F, 9F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(7F, 7F, 9F, 9F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(7F, 7F, 9F, 9F), Quadrant.R0)
    };

    static final RebarUv.Uv[] W14 = {
        new RebarUv.Uv(new CuboidFace.UVs(7F, 16.016F, 9F, -0.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(7F, -0.016F, 9F, 16.016F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(7F, 7F, 9F, 9F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(7F, 7F, 9F, 9F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016F, 7F, 16.016F, 9F), Quadrant.R0),
        new RebarUv.Uv(new CuboidFace.UVs(-0.016000748F, 7F, 16.016F, 9F), Quadrant.R0)
    };

    private static final int[] WINDOWS = {0x082A0, 0x0A280, 0x05173, 0x05270, 0x05A78, 0x0597B};

    private static float pick(
            int code, int slot, float x0, float y0, float z0, float x1, float y1, float z1) {
        int sel = code >>> (4 * slot) & 0xF;
        float low =
                switch (sel >>> 2) {
                    case 0 -> x0;
                    case 1 -> y0;
                    default -> z0;
                };
        float high =
                switch (sel >>> 2) {
                    case 0 -> x1;
                    case 1 -> y1;
                    default -> z1;
                };
        float value = (sel & 2) == 0 ? low : high;
        return (sel & 1) == 0 ? value : 16F - value;
    }

    static Uv[] windows(float x0, float y0, float z0, float x1, float y1, float z1) {
        Uv[] windows = new Uv[6];
        for (Direction dir : Direction.VALUES) {
            int code = WINDOWS[dir.ordinal()];
            if (code < 0) throw new IllegalStateException("no uv window for " + dir);
            windows[dir.ordinal()] =
                    new Uv(
                            new CuboidFace.UVs(
                                    pick(code, 0, x0, y0, z0, x1, y1, z1),
                                    pick(code, 1, x0, y0, z0, x1, y1, z1),
                                    pick(code, 2, x0, y0, z0, x1, y1, z1),
                                    pick(code, 3, x0, y0, z0, x1, y1, z1)),
                            (code & 0x10000) != 0 ? Quadrant.R90 : Quadrant.R0);
        }
        return windows;
    }
}
