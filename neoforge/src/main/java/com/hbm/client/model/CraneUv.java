// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.math.Quadrant;
import net.minecraft.client.resources.model.cuboid.CuboidFace;

final class CraneUv {

    private CraneUv() {}

    record Uv(CuboidFace.UVs uvs, Quadrant quadrant) {}

    static final CraneUv.Uv[][] WINDOWS = {
        {
            new CraneUv.Uv(new CuboidFace.UVs(0F, 16F, 16F, 0F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0)
        },
        {
            new CraneUv.Uv(new CuboidFace.UVs(0F, 16F, 16F, 0F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R90),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0)
        },
        {
            new CraneUv.Uv(new CuboidFace.UVs(0F, 16F, 16F, 0F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(16F, 16F, 0F, 0F), Quadrant.R90),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0)
        },
        {
            new CraneUv.Uv(new CuboidFace.UVs(0F, 16F, 16F, 0F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(16F, 16F, 0F, 0F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
            new CraneUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0)
        }
    };
}
