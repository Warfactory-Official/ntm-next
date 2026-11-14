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

import com.hbm.blockentity.machine.DeuteriumExtractorBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.NumberFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class DeuteriumExtractorBlock extends Block
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {

    public DeuteriumExtractorBlock(Properties props) {
        super(props);
    }

    static void buildOverlay(DeuteriumExtractorBlockEntity be, Block block, LookOverlay info) {
        info.title(Component.empty().append(Component.translatable(block.getDescriptionId())));
        boolean starved = be.energy < be.getMaxPower() / 20;
        info.row(
                Component.translatable(
                        "overlay.hbm.power", NumberFormatter.formatCompact(be.energy)),
                starved ? 0xFF5555 : 0x55FF55);
        for (int i = 0; i < be.tankCount(); i++) {
            FluidTankNTM tank = be.tank(i);
            String arrow = i < 1 ? ChatFormatting.GREEN + "-> " : ChatFormatting.RED + "<- ";
            info.row(
                    Component.literal(arrow)
                            .append(
                                    Component.translatable(
                                            "overlay.hbm.tank",
                                            NTMFluidProperties.clientName(tank.getFluid()),
                                            tank.getFill(),
                                            tank.getMaxFill())));
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeuteriumExtractorBlockEntity(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (level.getBlockEntity(pos) instanceof DeuteriumExtractorBlockEntity be) {
            buildOverlay(be, this, info);
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.DEUTERIUM_EXTRACTOR)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .fe();
    }
}
