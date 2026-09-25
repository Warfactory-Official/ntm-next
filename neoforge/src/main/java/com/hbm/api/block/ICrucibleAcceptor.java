// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.block;

import com.hbm.inventory.material.Mats.MaterialStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface ICrucibleAcceptor {

    boolean canAcceptPartialPour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack);

    MaterialStack pour(Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack);

    boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack);

    MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack);
}
