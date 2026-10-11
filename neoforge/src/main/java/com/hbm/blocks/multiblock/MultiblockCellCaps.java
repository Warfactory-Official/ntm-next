// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.NtmContracts.Contract;
import com.hbm.capability.NtmContracts;
import com.hbm.platform.NeoForgeCaps;
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
            NeoForgeCaps.pendingBlocks.add(
                    new NeoForgeCaps.BlockRegistration<>(
                            EnergyCaps.RECEIVER,
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, side) ->
                                    NtmCapabilities.surfaceCap(
                                            level,
                                            pos,
                                            state,
                                            side,
                                            IEnergyHandlerMK2.class,
                                            NtmCapabilities.CapRole.POWER_IN)));
            NeoForgeCaps.pendingBlocks.add(
                    new NeoForgeCaps.BlockRegistration<>(
                            EnergyCaps.PROVIDER,
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, side) ->
                                    NtmCapabilities.surfaceCap(
                                            level,
                                            pos,
                                            state,
                                            side,
                                            IEnergyHandlerMK2.class,
                                            NtmCapabilities.CapRole.POWER_OUT)));
            NeoForgeCaps.pendingBlocks.add(
                    new NeoForgeCaps.BlockRegistration<>(
                            FluidCaps.RECEIVER,
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, face) ->
                                    NtmCapabilities.surfaceCap(
                                            level,
                                            pos,
                                            state,
                                            face,
                                            IFluidHandlerMK2.class,
                                            NtmCapabilities.CapRole.FLUID_IN)));
            NeoForgeCaps.pendingBlocks.add(
                    new NeoForgeCaps.BlockRegistration<>(
                            FluidCaps.PROVIDER,
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, face) ->
                                    NtmCapabilities.surfaceCap(
                                            level,
                                            pos,
                                            state,
                                            face,
                                            IFluidHandlerMK2.class,
                                            NtmCapabilities.CapRole.FLUID_OUT)));
            NeoForgeCaps.itemBlockExposures.add((Supplier<? extends Block>) cell);
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
