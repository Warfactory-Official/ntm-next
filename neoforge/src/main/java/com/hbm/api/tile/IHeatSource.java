// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IHeatSource {

    int getHeatStored(Level level, BlockPos pos);

    void useUpHeat(Level level, BlockPos pos, int heat);
}
