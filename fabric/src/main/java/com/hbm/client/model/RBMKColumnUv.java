// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.math.Quadrant;
import net.minecraft.client.resources.model.cuboid.CuboidFace;

final class RBMKColumnUv {

    private RBMKColumnUv() {}

    static final CuboidFace.UVs[] W0 = {
        new CuboidFace.UVs(0F, 0F, 16F, 16F),
        new CuboidFace.UVs(0F, 0F, 16F, 16F),
        new CuboidFace.UVs(0F, 0F, 16F, 16F),
        new CuboidFace.UVs(0F, 0F, 16F, 16F),
        new CuboidFace.UVs(0F, 0F, 16F, 16F),
        new CuboidFace.UVs(0F, 0F, 16F, 16F)
    };

    static final CuboidFace.UVs[] W1 = {
        new CuboidFace.UVs(0F, 0F, 16F, 16F),
        new CuboidFace.UVs(0F, 0F, 16F, 16F),
        new CuboidFace.UVs(0F, 12F, 16F, 16F),
        new CuboidFace.UVs(0F, 12F, 16F, 16F),
        new CuboidFace.UVs(0F, 12F, 16F, 16F),
        new CuboidFace.UVs(0F, 12F, 16F, 16F)
    };

    static final CuboidFace.UVs[] W2 = {
        new CuboidFace.UVs(1F, 9F, 7F, 15F),
        new CuboidFace.UVs(1F, 1F, 7F, 7F),
        new CuboidFace.UVs(9F, 14F, 15F, 16F),
        new CuboidFace.UVs(1F, 14F, 7F, 16F),
        new CuboidFace.UVs(1F, 14F, 7F, 16F),
        new CuboidFace.UVs(9F, 14F, 15F, 16F)
    };

    static final CuboidFace.UVs[] W3 = {
        new CuboidFace.UVs(1F, 1F, 7F, 7F),
        new CuboidFace.UVs(1F, 9F, 7F, 15F),
        new CuboidFace.UVs(9F, 14F, 15F, 16F),
        new CuboidFace.UVs(1F, 14F, 7F, 16F),
        new CuboidFace.UVs(9F, 14F, 15F, 16F),
        new CuboidFace.UVs(1F, 14F, 7F, 16F)
    };

    static final CuboidFace.UVs[] W4 = {
        new CuboidFace.UVs(9F, 1F, 15F, 7F),
        new CuboidFace.UVs(9F, 9F, 15F, 15F),
        new CuboidFace.UVs(1F, 14F, 7F, 16F),
        new CuboidFace.UVs(9F, 14F, 15F, 16F),
        new CuboidFace.UVs(9F, 14F, 15F, 16F),
        new CuboidFace.UVs(1F, 14F, 7F, 16F)
    };

    static final CuboidFace.UVs[] W5 = {
        new CuboidFace.UVs(9F, 9F, 15F, 15F),
        new CuboidFace.UVs(9F, 1F, 15F, 7F),
        new CuboidFace.UVs(1F, 14F, 7F, 16F),
        new CuboidFace.UVs(9F, 14F, 15F, 16F),
        new CuboidFace.UVs(1F, 14F, 7F, 16F),
        new CuboidFace.UVs(9F, 14F, 15F, 16F)
    };
}
