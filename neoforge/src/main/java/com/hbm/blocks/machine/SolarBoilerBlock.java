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

import com.hbm.blockentity.machine.SolarBoilerBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.Blink;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class SolarBoilerBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {
    public SolarBoilerBlock(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof SolarBoilerBlockEntity boiler)) return;
        info.title(getName());

        FluidTankNTM[] tanks = {boiler.water, boiler.steam};
        for (int i = 0; i < tanks.length; i++) {
            FluidTankNTM tank = tanks[i];
            String arrow = i == 0 ? ChatFormatting.GREEN + "-> " : ChatFormatting.RED + "<- ";
            info.row(
                    Component.literal(arrow)
                            .append(
                                    Component.translatable(
                                            "overlay.hbm.tank",
                                            NTMFluidProperties.clientName(tank.getFluid()),
                                            tank.getFill(),
                                            tank.getMaxFill())));
        }

        if (boiler.display < 1) {
            info.row(
                    Component.translatable("overlay.hbm.too_cold"),
                    Blink.lit() ? 0xFF0000 : 0xFFFF00);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarBoilerBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut();
    }
}
