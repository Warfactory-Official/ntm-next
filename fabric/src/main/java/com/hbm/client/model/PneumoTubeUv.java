// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.math.Quadrant;
import net.minecraft.client.resources.model.cuboid.CuboidFace;

final class PneumoTubeUv {

    private PneumoTubeUv() {}

    static final PneumoTubeUv.Uv[] STRAIGHT_X = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 11F, 16F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 5F, 16F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 5F, 16F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 5F, 16F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] STRAIGHT_Z = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 11F, 16F, 5F), Quadrant.R90),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(16F, 11F, 0F, 5F), Quadrant.R90),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 5F, 16F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 5F, 16F, 11F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] STRAIGHT_Y = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 11F, 16F, 5F), Quadrant.R90),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(16F, 11F, 0F, 5F), Quadrant.R90),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(16F, 11F, 0F, 5F), Quadrant.R90),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 11F, 16F, 5F), Quadrant.R90)
    };
    static final PneumoTubeUv.Uv[] CORE = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_EAST_BODY = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 11F, 12F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 5F, 12F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 5F, 5F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 5F, 12F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_EAST_HEAD = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 4F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(12F, 4F, 16F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 4F, 4F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(12F, 4F, 16F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 12F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_WEST_BODY = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 11F, 5F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 5F, 5F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 5F, 12F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 5F, 5F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_WEST_HEAD = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 4F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 4F, 4F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(12F, 4F, 16F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 4F, 4F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 12F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_UP_BODY = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 4F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 4F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 4F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 4F, 11F, 5F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_UP_HEAD = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 4F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 4F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 4F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 4F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 4F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_DOWN_BODY = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 12F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_DOWN_HEAD = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 4F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_SOUTH_BODY = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 12F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 5F, 12F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 5F, 5F, 11F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_SOUTH_HEAD = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 16F, 12F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(12F, 4F, 16F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 4F, 4F, 12F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_NORTH_BODY = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 4F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 4F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 5F, 5F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 5F, 12F, 11F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] NOZZLE_NORTH_HEAD = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 0F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 4F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 4F, 4F, 12F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(12F, 4F, 16F, 12F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] ARM_EAST = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 11F, 16F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 5F, 16F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 5F, 5F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 5F, 16F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] ARM_WEST = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 11F, 5F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 5F, 5F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 5F, 16F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 5F, 5F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] ARM_UP = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 0F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 0F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 0F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 0F, 11F, 5F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] ARM_DOWN = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 16F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 16F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 16F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 16F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] ARM_SOUTH = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 16F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 11F, 11F, 16F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 5F, 16F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 5F, 5F, 11F), Quadrant.R0)
    };
    static final PneumoTubeUv.Uv[] ARM_NORTH = {
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 0F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 0F, 11F, 5F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(5F, 5F, 11F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(0F, 5F, 5F, 11F), Quadrant.R0),
        new PneumoTubeUv.Uv(new CuboidFace.UVs(11F, 5F, 16F, 11F), Quadrant.R0)
    };

    record Uv(CuboidFace.UVs uvs, Quadrant quadrant) {}
}
