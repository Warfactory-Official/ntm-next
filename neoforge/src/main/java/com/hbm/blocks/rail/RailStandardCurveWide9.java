// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

public class RailStandardCurveWide9 extends RailStandardCurveBase {

    static final int[][] STAIRCASE_WIDE9 = {
        {1, 0, 0},
        {2, 0, 0},
        {0, 1, 1},
        {1, 1, 0},
        {2, 1, 0},
        {3, 1, 0},
        {4, 1, 0},
        {2, 2, 1},
        {3, 2, 1},
        {4, 2, 1},
        {5, 2, 0},
        {4, 3, 1},
        {5, 3, 1},
        {5, 4, 1},
        {6, 3, 0},
        {6, 4, 0},
        {7, 4, 0},
        {6, 5, 1},
        {7, 5, 1},
        {6, 6, 1},
        {7, 6, 1},
        {7, 7, 1},
        {7, 8, 1},
        {8, 6, 0},
        {8, 7, 0},
        {8, 8, 0},
    };

    public RailStandardCurveWide9(Properties props) {
        super(props);
        this.width = 8;
    }

    @Override
    protected int[][] staircase() {
        return STAIRCASE_WIDE9;
    }
}
