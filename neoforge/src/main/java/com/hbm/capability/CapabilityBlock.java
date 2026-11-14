/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.capability;

import com.hbm.registration.RegistryHandle;
import com.hbm.uninos.graph.PhysicalGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public interface CapabilityBlock extends PhysicalConnector {

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

    default boolean bulkCapabilityRelevant(BlockState state) {
        return state.hasBlockEntity();
    }
}
