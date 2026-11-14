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

package com.hbm.blocks.multiblock;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidHandler;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.NtmContracts.Contract;
import com.hbm.capability.NtmContracts;
import com.hbm.platform.Caps;
import com.hbm.registration.RegistryHandle;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;

public final class MultiblockCellCaps {

    private static final List<RegistryHandle<? extends Block>> CELLS = ModBlocks.cellHandles();

    private MultiblockCellCaps() {}

    public static void declare(List<RegistryHandle<? extends Block>> roster) {
        List<RegistryHandle<? extends Block>> selfCelled = selfCelled(roster);
        for (Contract<?> contract :
                List.of(
                        NtmContracts.CRUCIBLE_ACCEPTOR,
                        NtmContracts.CONVEYOR_BELT,
                        NtmContracts.ENTERABLE)) {
            proxy(contract, MultiblockSurface::coreOfFoldedCell);
        }
        proxy(NtmContracts.HEAT_SOURCE, MultiblockSurface::heatCoreOfCell);
        List<RegistryHandle<? extends Block>> everyCell =
                roster.stream()
                        .filter(handle -> handle.get() instanceof MultiblockCellBlock)
                        .toList();
        for (Contract<?> contract :
                List.of(
                        NtmContracts.LOOK_OVERLAY,
                        NtmContracts.TOOLABLE,
                        NtmContracts.BOMB,
                        NtmContracts.COPIABLE,
                        NtmContracts.REPAIRABLE,
                        NtmContracts.EXTINGUISHABLE)) {
            for (RegistryHandle<? extends Block> cell : everyCell) {
                NtmContracts.proxy(contract, cell, MultiblockSurface::coreOfFoldedCell);
            }
        }
        for (Contract<?> contract :
                List.of(NtmContracts.ROR_VALUE_PROVIDER, NtmContracts.ROR_INTERACTIVE)) {
            proxy(contract, selfCelled, MultiblockSurface::rorCoreOfCell);
        }
        proxy(
                NtmContracts.INVENTORY,
                selfCelled,
                (level, pos, state) ->
                        MultiblockSurface.proxyCoreOfCell(
                                level, pos, state, MultiblockCoreBlock.PASSIVE_ITEMS));
        for (RegistryHandle<? extends Block> cell : CELLS) {
            Caps.pendingBlocks.add(
                    new Caps.BlockRegistration<>(
                            EnergyCaps.RECEIVER,
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, side) ->
                                    NtmCapabilities.surfaceCap(
                                            level,
                                            pos,
                                            state,
                                            side,
                                            EnergyHandler.class,
                                            NtmCapabilities.CapRole.POWER_IN)));
            Caps.pendingBlocks.add(
                    new Caps.BlockRegistration<>(
                            EnergyCaps.PROVIDER,
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, side) ->
                                    NtmCapabilities.surfaceCap(
                                            level,
                                            pos,
                                            state,
                                            side,
                                            EnergyHandler.class,
                                            NtmCapabilities.CapRole.POWER_OUT)));
            Caps.pendingBlocks.add(
                    new Caps.BlockRegistration<>(
                            FluidCaps.RECEIVER,
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, face) ->
                                    NtmCapabilities.surfaceCap(
                                            level,
                                            pos,
                                            state,
                                            face,
                                            FluidHandler.class,
                                            NtmCapabilities.CapRole.FLUID_IN)));
            Caps.pendingBlocks.add(
                    new Caps.BlockRegistration<>(
                            FluidCaps.PROVIDER,
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, face) ->
                                    NtmCapabilities.surfaceCap(
                                            level,
                                            pos,
                                            state,
                                            face,
                                            FluidHandler.class,
                                            NtmCapabilities.CapRole.FLUID_OUT)));
            Caps.itemBlockExposures.add((Supplier<? extends Block>) cell);
            NtmCapabilities.exposeEnergyAtBlock(cell);
            NtmCapabilities.exposeFluid(cell);
            FusionPorts.declareAtCell(cell);
        }
    }

    private static List<RegistryHandle<? extends Block>> selfCelled(
            List<RegistryHandle<? extends Block>> roster) {
        return roster.stream()
                .filter(
                        handle ->
                                handle.get() instanceof MultiblockCoreBlock core
                                        && !core.usesSharedCells())
                .toList();
    }

    private static void proxy(Contract<?> contract, NtmContracts.OwnerLookup owner) {
        for (RegistryHandle<? extends Block> cell : CELLS) {
            NtmContracts.proxy(contract, cell, owner);
        }
    }

    private static void proxy(
            Contract<?> contract,
            List<RegistryHandle<? extends Block>> selfCelled,
            NtmContracts.OwnerLookup owner) {
        proxy(contract, owner);
        for (RegistryHandle<? extends Block> core : selfCelled) {
            NtmContracts.proxy(contract, core, owner);
        }
    }
}
