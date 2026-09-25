// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.math.Quadrant;
import net.minecraft.client.resources.model.cuboid.CuboidFace;

final class ConveyorChuteUv {

    private ConveyorChuteUv() {}

    static final ConveyorUv.Uv[][] BELT_SOUTH = {
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 4F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(16F, 4F, 0F, 0F), Quadrant.R0)
        },
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(16F, 4F, 12F, 12F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 4F, 4F, 12F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 4F, 4F, 0F), Quadrant.R0)
        },
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 4F, 0F, 12F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 4F, 16F, 12F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 4F, 4F, 0F), Quadrant.R0)
        }
    };
    static final ConveyorUv.Uv[][] BELT_EAST = {
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(16F, 4F, 0F, 12F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(16F, 12F, 0F, 4F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 4F, 4F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0)
        },
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 4F, 16F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 4F, 4F, 0F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(16F, 4F, 12F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
        },
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 4F, 4F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 16F, 4F, 12F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 4F, 0F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
        }
    };
    static final ConveyorUv.Uv[][] BELT_WEST = {
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 4F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 4F, 16F, 12F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 4F, 4F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0)
        },
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 4F, 12F, 0F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(16F, 4F, 12F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
        },
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 16F, 12F, 12F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 4F), Quadrant.R90),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 4F, 0F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
        }
    };
    static final ConveyorUv.Uv[][] BELT_NORTH = {
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 16F, 12F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 16F, 4F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(16F, 4F, 0F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 16F, 16F), Quadrant.R0)
        },
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 4F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(16F, 12F, 12F, 4F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 4F, 4F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
        },
        {
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 4F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 0F, 4F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(12F, 4F, 4F, 0F), Quadrant.R0),
            new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
        }
    };
    static final ConveyorUv.Uv[] POST_MIN_MIN = {
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 4F, 4F, 0F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] POST_MAX_MIN = {
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 4F, 16F, 0F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] POST_MIN_MAX = {
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 16F, 4F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 0F, 4F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] POST_MAX_MAX = {
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 16F, 16F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 0F, 16F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] STUB_WEST = {
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 2F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 14F, 12F, 16F), Quadrant.R90),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 2F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 2F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] STUB_EAST = {
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 12F, 16F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 16F, 4F, 14F), Quadrant.R90),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] STUB_NORTH = {
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 2F, 12F, 0F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(12F, 16F, 4F, 14F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 2F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(0F, 12F, 2F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] STUB_SOUTH = {
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 16F, 12F, 14F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 14F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 12F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 12F, 16F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 12F, 16F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] PANE_WEST_LIP = {
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 12F, 2F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 4F, 2F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 0F, 2F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 0F, 2F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 12F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] PANE_WEST_BASE = {
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 12F, 2F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 4F, 2F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 0F, 2F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 0F, 2F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] PANE_EAST_LIP = {
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 12F, 14F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 4F, 14F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 0F, 14F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 0F, 14F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 12F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] PANE_EAST_BASE = {
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 12F, 14F, 4F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 4F, 14F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 0F, 14F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 0F, 14F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] PANE_NORTH_LIP = {
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 2F, 12F, 2F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 2F, 12F, 2F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 0F, 2F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 0F, 2F, 12F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] PANE_NORTH_BASE = {
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 2F, 12F, 2F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 2F, 12F, 2F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 0F, 2F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(2F, 0F, 2F, 16F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] PANE_SOUTH_LIP = {
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 14F, 12F, 14F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 14F, 12F, 14F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 0F, 14F, 12F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 0F, 14F, 12F), Quadrant.R0)
    };
    static final ConveyorUv.Uv[] PANE_SOUTH_BASE = {
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 14F, 12F, 14F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 14F, 12F, 14F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(4F, 0F, 12F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 0F, 14F, 16F), Quadrant.R0),
        new ConveyorUv.Uv(new CuboidFace.UVs(14F, 0F, 14F, 16F), Quadrant.R0)
    };
}
