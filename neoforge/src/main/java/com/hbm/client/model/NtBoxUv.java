// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.math.Quadrant;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.core.Direction;

final class NtBoxUv {

    private NtBoxUv() {}

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

    record Uv(CuboidFace.UVs uvs, Quadrant quadrant) {}

    static Uv window(Direction dir, float x0, float y0, float z0, float x1, float y1, float z1) {
        int code = WINDOWS[dir.ordinal()];
        return new Uv(
                new CuboidFace.UVs(
                        pick(code, 0, x0, y0, z0, x1, y1, z1),
                        pick(code, 1, x0, y0, z0, x1, y1, z1),
                        pick(code, 2, x0, y0, z0, x1, y1, z1),
                        pick(code, 3, x0, y0, z0, x1, y1, z1)),
                (code & 0x10000) != 0 ? Quadrant.R90 : Quadrant.R0);
    }
}
