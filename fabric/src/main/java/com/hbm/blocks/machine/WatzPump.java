// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockCore;

public class WatzPump extends BlockMultiblockCore {

    private static final int[] DIMENSIONS = {1, 0, 0, 0, 0, 0};

    public WatzPump(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }
}
