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

package com.hbm.blocks.machine;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidHandler;
import com.hbm.blockentity.machine.CustomMachineBlockEntity;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.platform.Caps;
import com.hbm.registration.RegistryHandle;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CMPortBlock extends Block implements CapabilityBlock {

    private static final MachineCaps CAPS =
            MachineCaps.blockKeyed()
                    .powerIn()
                    .powerOut()
                    .fluidIn()
                    .fluidOut()
                    .itemsAtCells()
                    .selfProvided();

    public CMPortBlock(Properties properties) {
        super(properties);
    }

    private static @Nullable CustomMachineBlockEntity owner(
            Level level, BlockPos pos, BlockState state) {
        return CustomMachinePorts.ownerAt(level, pos, state);
    }

    @Override
    public MachineCaps caps() {
        return CAPS;
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        EnergyCaps.RECEIVER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, side) -> (EnergyHandler) owner(level, pos, state)));
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        EnergyCaps.PROVIDER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, side) -> (EnergyHandler) owner(level, pos, state)));
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        FluidCaps.RECEIVER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, face) -> (FluidHandler) owner(level, pos, state)));
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        FluidCaps.PROVIDER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, face) -> (FluidHandler) owner(level, pos, state)));
    }

    public boolean bulkPortRelevant(BlockState ignoredState) {
        return true;
    }

    public boolean bulkSnapshotCell(BlockState ignoredState) {
        return true;
    }
}
