// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blockentity.machine.BlockEntityCustomMachine;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.platform.NeoForgeCaps;
import com.hbm.registration.RegistryHandle;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockCMPort extends Block implements ICapabilityBlock {

    private static final MachineCaps CAPS =
            MachineCaps.blockKeyed()
                    .powerIn()
                    .powerOut()
                    .fluidIn()
                    .fluidOut()
                    .itemsAtCells()
                    .selfProvided();

    public BlockCMPort(Properties properties) {
        super(properties);
    }

    private static @Nullable BlockEntityCustomMachine owner(
            Level level, BlockPos pos, BlockState state) {
        return CustomMachinePorts.ownerAt(level, pos, state);
    }

    @Override
    public MachineCaps caps() {
        return CAPS;
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {
        NeoForgeCaps.pendingBlocks.add(
                new NeoForgeCaps.BlockRegistration<>(
                        EnergyCaps.RECEIVER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, side) ->
                                (IEnergyHandlerMK2) owner(level, pos, state)));
        NeoForgeCaps.pendingBlocks.add(
                new NeoForgeCaps.BlockRegistration<>(
                        EnergyCaps.PROVIDER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, side) ->
                                (IEnergyHandlerMK2) owner(level, pos, state)));
        NeoForgeCaps.pendingBlocks.add(
                new NeoForgeCaps.BlockRegistration<>(
                        FluidCaps.RECEIVER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, face) ->
                                (IFluidHandlerMK2) owner(level, pos, state)));
        NeoForgeCaps.pendingBlocks.add(
                new NeoForgeCaps.BlockRegistration<>(
                        FluidCaps.PROVIDER,
                        (Supplier<? extends Block>) self,
                        (level, pos, state, be, face) ->
                                (IFluidHandlerMK2) owner(level, pos, state)));
    }
}
