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

import com.hbm.api.environment.Environment;
import com.hbm.blockentity.machine.thermal.SteamCondenserBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.Blink;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CondenserBlock extends Block
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {

    public CondenserBlock(Properties props) {
        super(props);
    }

    private static void line(LookOverlay info, String prefix, FluidTankNTM tank, int color) {
        info.row(
                Component.literal(prefix)
                        .append(NTMFluidProperties.getDisplayName(tank.getFluid()))
                        .append(": ")
                        .append(
                                Component.translatable(
                                        "unit.millibuckets",
                                        tank.getFill() + "/" + tank.getMaxFill())),
                color);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamCondenserBlockEntity(pos, state);
    }

    static void vacuumWarning(Level level, LookOverlay info) {
        if (!Environment.vacuum(level)) return;
        info.row(
                Component.literal("! ! ! ")
                        .append(Component.translatable("atmosphere.noVacuum"))
                        .append(" ! ! !"),
                Blink.lit() ? 0xFF0000 : 0xFFFF00);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof SteamCondenserBlockEntity be)) return;
        info.title(getName());
        vacuumWarning(level, info);
        line(info, "-> ", be.input, 0x00FF00);
        line(info, "<- ", be.output, 0xFF0000);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CONDENSER).fluidIn().fluidOut();
    }
}
