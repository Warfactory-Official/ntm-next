// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class RailStandardSwitchFlipped extends RailStandardSwitch {

    private static final int[][] SIDE_TRACK_FLIPPED = {
        {2, 1}, {3, 1}, {4, 1}, {5, 1}, {4, 2}, {5, 2}, {5, 3}, {6, 2}, {6, 3}, {7, 2}, {7, 3},
        {7, 4}, {8, 3}, {9, 3}, {10, 3}, {11, 3}, {12, 3}, {13, 3}, {14, 3}, {8, 4}, {9, 4},
        {10, 4}, {11, 4}, {12, 4}, {13, 4}, {14, 4},
    };

    public RailStandardSwitchFlipped(Properties props) {
        super(props);

        railDefs.clear();

        RailDef main = new RailDef("main");
        RailDef side = new RailDef("side");
        railDefs.add(main);
        railDefs.add(side);

        main.nodes.add(new Vec3(-8.5, 0.1875, 0.5));
        main.nodes.add(new Vec3(-7.5, 0.1875, 0.5));
        main.nodes.add(new Vec3(6.5, 0.1875, 0.5));
        main.nodes.add(new Vec3(7.5, 0.1875, 0.5));
        main.nodes.add(new Vec3(8.5, 0.1875, 0.5));

        side.nodes.add(new Vec3(-8.5, 0.1875, -3.5));
        side.nodes.add(new Vec3(-7.5, 0.1875, -3.5));
        side.nodes.add(new Vec3(-6.5, 0.1875, -3.5));
        side.nodes.add(new Vec3(-5.5, 0.1875, -3.5));
        side.nodes.add(new Vec3(-4.5, 0.1875, -3.5));
        side.nodes.add(new Vec3(-3.5, 0.1875, -3.5));
        side.nodes.add(new Vec3(-2.5, 0.1875, -3.5));
        side.nodes.add(new Vec3(-1.5, 0.1875, -3.5));
        side.nodes.add(new Vec3(-0.5, 0.1875, -3.25));
        side.nodes.add(new Vec3(0.5, 0.1875, -2.9375));
        side.nodes.add(new Vec3(1.5, 0.1875, -2.375));
        side.nodes.add(new Vec3(2.5, 0.1875, -1.4625));
        side.nodes.add(new Vec3(3.5, 0.1875, -0.75));
        side.nodes.add(new Vec3(4.5, 0.1875, -0.1875));
        side.nodes.add(new Vec3(5.5, 0.1875, 0.175));
        side.nodes.add(new Vec3(6.5, 0.1875, 0.375));
        side.nodes.add(new Vec3(7.5, 0.1875, 0.5));
        side.nodes.add(new Vec3(8.5, 0.1875, 0.5));
    }

    @Override
    protected Direction rotFor(Direction facing) {
        return facing.getCounterClockWise();
    }

    @Override
    protected int[][] sideTrack() {
        return SIDE_TRACK_FLIPPED;
    }
}
