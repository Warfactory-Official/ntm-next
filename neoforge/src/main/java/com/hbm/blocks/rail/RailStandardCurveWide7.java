// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

public class RailStandardCurveWide7 extends RailStandardCurveBase {

    static final int[][] STAIRCASE_WIDE7 = {
        {1, 0, 0},
        {2, 0, 0},
        {0, 1, 1},
        {1, 1, 1},
        {2, 1, 1},
        {3, 1, 0},
        {4, 1, 0},
        {2, 2, 1},
        {3, 2, 0},
        {4, 2, 0},
        {5, 2, 0},
        {3, 3, 1},
        {4, 3, 0},
        {5, 3, 0},
        {4, 4, 1},
        {5, 4, 0},
        {6, 4, 0},
        {5, 5, 1},
        {5, 6, 1},
        {6, 5, 1},
        {6, 6, 1},
    };

    public RailStandardCurveWide7(Properties props) {
        super(props);
        this.width = 6;
    }

    @Override
    protected int[][] staircase() {
        return STAIRCASE_WIDE7;
    }
}
