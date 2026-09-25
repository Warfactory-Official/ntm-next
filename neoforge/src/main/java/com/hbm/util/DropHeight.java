// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;

public final class DropHeight {

    private static final int CLEARANCE = 32;

    private DropHeight() {}

    public static double clearOfTerrain(LevelReader level, double legacy, double x, double z) {
        return Math.max(
                legacy,
                level.getHeight(
                                Heightmap.Types.MOTION_BLOCKING,
                                (int) Math.floor(x),
                                (int) Math.floor(z))
                        + CLEARANCE);
    }
}
