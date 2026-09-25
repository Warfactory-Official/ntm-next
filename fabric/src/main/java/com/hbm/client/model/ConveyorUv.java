// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.math.Quadrant;
import net.minecraft.client.resources.model.cuboid.CuboidFace;

final class ConveyorUv {

    private ConveyorUv() {}

    record Uv(CuboidFace.UVs uvs, Quadrant quadrant) {}

    static final ConveyorUv.Uv[] W0 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(16F, 0F, 0F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(16F, 4F, 0F, 0F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W1 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(16F, 0F, 0F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W2 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 16F, 4F, 0F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 8F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 8F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 8F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 8F, 16F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W3 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 16F, 16F, 0F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 8F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 8F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 8F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 8F, 16F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W4 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 6F, 12F, 0F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 6F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 4F, 4F, 0F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 6F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 6F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W5 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 0F), Quadrant.R90),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R90),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W6 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 4F, 4F), Quadrant.R90),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 16F, 4F, 12F), Quadrant.R90),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W7 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 4F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W8 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 4F, 16F, 0F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W9 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 16F, 4F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W10 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 16F, 16F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W11 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 6F, 12F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 6F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 6F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 6F, 16F), Quadrant.R0)
    };

    static final ConveyorUv.Uv[] W12 = {
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 6F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 6F, 12F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(6F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(6F, 12F, 12F, 16F), Quadrant.R0)
    };
}
