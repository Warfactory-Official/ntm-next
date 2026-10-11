// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import com.hbm.capability.PhysicalConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public final class PhysicalPortBlock extends Block implements PhysicalConnector {
    private final boolean power;

    private PhysicalPortBlock(Properties properties, boolean power) {
        super(properties);
        this.power = power;
    }

    public static Block power(Properties properties) {
        return new PhysicalPortBlock(properties, true);
    }

    public static Block fluid(Properties properties) {
        return new PhysicalPortBlock(properties, false);
    }

    @Override
    public boolean hasPowerContact(Level level, BlockPos pos, BlockState state, Direction side) {
        return power;
    }

    @Override
    public boolean hasFluidContact(
            Level level, BlockPos pos, BlockState state, Direction side, Fluid fluid) {
        return !power;
    }
}
