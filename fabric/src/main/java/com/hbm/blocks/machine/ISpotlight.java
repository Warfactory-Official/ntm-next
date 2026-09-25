// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface ISpotlight {

    int getBeamLength();

    boolean lights(BlockState state, Direction dir);
}
