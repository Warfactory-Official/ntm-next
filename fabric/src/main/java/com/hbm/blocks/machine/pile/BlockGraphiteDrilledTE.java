// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ITickingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public abstract class BlockGraphiteDrilledTE extends BlockGraphiteDrilledBase
        implements ITickingBlock {

    protected BlockGraphiteDrilledTE(BlockBehaviour.Properties props) {
        super(props);
    }
}
