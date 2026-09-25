// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.NtmContracts.Contract;
import com.hbm.capability.NtmContracts;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.machine.fusion.FusionPorts;
import java.util.List;
import net.minecraft.world.level.block.Block;

public final class MultiblockCellCaps {

    private static final List<RegistryHandle<? extends Block>> CELLS = ModBlocks.cellHandles();

    private MultiblockCellCaps() {}

    public static void declare(List<RegistryHandle<? extends Block>> roster) {
        List<RegistryHandle<? extends Block>> selfCelled = selfCelled(roster);
        for (Contract<?> contract :
                List.of(
                        NtmContracts.CRUCIBLE_ACCEPTOR,
                        NtmContracts.HEAT_SOURCE,
                        NtmContracts.CONVEYOR_BELT,
                        NtmContracts.ENTERABLE)) {
            proxy(contract, MultiblockSurface::coreOfFoldedCell);
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
                                level, pos, state, BlockMultiblockCore.PASSIVE_ITEMS));
        for (RegistryHandle<? extends Block> cell : CELLS) {

            Services.CAPS.registerBlockProvider(
                    EnergyCaps.RECEIVER,
                    cell,
                    (level, pos, state, be, side) ->
                            NtmCapabilities.fullSurfaceCap(
                                    level,
                                    pos,
                                    state,
                                    side,
                                    IEnergyHandlerMK2.class,
                                    NtmCapabilities.CapRole.POWER_IN));
            Services.CAPS.registerBlockProvider(
                    EnergyCaps.PROVIDER,
                    cell,
                    (level, pos, state, be, side) ->
                            NtmCapabilities.fullSurfaceCap(
                                    level,
                                    pos,
                                    state,
                                    side,
                                    IEnergyHandlerMK2.class,
                                    NtmCapabilities.CapRole.POWER_OUT));
            Services.CAPS.registerBlockProvider(
                    FluidCaps.RECEIVER,
                    cell,
                    (level, pos, state, be, face) ->
                            NtmCapabilities.fullSurfaceCap(
                                    level,
                                    pos,
                                    state,
                                    face.side(),
                                    IFluidHandlerMK2.class,
                                    NtmCapabilities.CapRole.FLUID_IN));
            Services.CAPS.registerBlockProvider(
                    FluidCaps.PROVIDER,
                    cell,
                    (level, pos, state, be, face) ->
                            NtmCapabilities.fullSurfaceCap(
                                    level,
                                    pos,
                                    state,
                                    face.side(),
                                    IFluidHandlerMK2.class,
                                    NtmCapabilities.CapRole.FLUID_OUT));
            Services.CAPS.exposeItemAtBlock(cell);
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
                                handle.get() instanceof BlockMultiblockCore core
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
