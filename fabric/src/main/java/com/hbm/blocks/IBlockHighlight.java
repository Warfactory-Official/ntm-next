// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import com.hbm.world.phys.SilhouetteMesh;
import net.minecraft.world.level.block.state.BlockState;

public interface IBlockHighlight {
    SilhouetteMesh visualOutline(BlockState state);
}
