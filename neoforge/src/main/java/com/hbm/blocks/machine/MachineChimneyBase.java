// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;

public abstract class MachineChimneyBase extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {

    protected MachineChimneyBase(Properties props) {
        super(props);
    }
}
