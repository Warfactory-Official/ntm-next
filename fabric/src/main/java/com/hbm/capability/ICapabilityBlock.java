// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.registration.RegistryHandle;
import com.hbm.uninos.graph.PhysicalGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public interface ICapabilityBlock extends PhysicalConnector {

    @Override
    default boolean hasPowerContact(Level level, BlockPos pos, BlockState state, Direction side) {
        return PhysicalGraph.powerEndpoint(level, pos, state, side);
    }

    @Override
    default boolean hasFluidContact(
            Level level, BlockPos pos, BlockState state, Direction side, Fluid fluid) {
        return PhysicalGraph.fluidEndpoint(level, pos, state, side, fluid);
    }

    MachineCaps caps();

    default void declareExtraCaps(RegistryHandle<? extends Block> self) {}
}
