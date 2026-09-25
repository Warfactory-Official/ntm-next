// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

import net.minecraft.world.level.Level;

public class RailNarrowCurve extends RailStandardCurveBase {

    public RailNarrowCurve(Properties props) {
        super(props);
    }

    @Override
    protected double turnRadius() {
        return 4.5D;
    }

    @Override
    protected double lift() {
        return 0;
    }

    @Override
    public TrackGauge getGauge(Level level, int x, int y, int z) {
        return TrackGauge.NARROW;
    }
}
