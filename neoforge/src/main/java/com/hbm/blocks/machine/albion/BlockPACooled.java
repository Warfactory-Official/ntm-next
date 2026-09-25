// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.albion;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;

public abstract class BlockPACooled extends BlockMultiblockCore implements ITickingBlock {

    protected BlockPACooled(Properties props) {
        super(props);
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getHeightOffset() {
        return 1;
    }

    @Override
    public int cellSound() {
        return BlockMultiblockCell.SOUND_METAL;
    }
}
