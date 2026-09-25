// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.conveyor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface IEnterableBlock {

    boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity);

    void onItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity);

    boolean canPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity);

    void onPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity);
}
