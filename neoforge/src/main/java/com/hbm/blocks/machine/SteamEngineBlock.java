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

import com.hbm.blockentity.machine.SteamEngineBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.util.NumberFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SteamEngineBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock, LookOverlayProvider {
    public SteamEngineBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed()
                .powerOut()
                .fluidIn()
                .fluidOut()
                .fe()
                .faces(
                        SteamEngineBlockEntity.class,
                        (_, side) -> side.getAxis().isHorizontal());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamEngineBlockEntity(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay card) {
        var engine = (SteamEngineBlockEntity) level.getBlockEntity(pos);
        var input = engine.tanks[0];
        var output = engine.tanks[1];
        card.title(getName());
        card.row(
                LookOverlay.inflow(
                        Component.translatable(
                                "overlay.hbm.tank_spaced",
                                NTMFluidProperties.getDisplayName(input.getFluid()),
                                NumberFormatter.formatGrouped(input.getFill()),
                                NumberFormatter.formatGrouped(input.getMaxFill()))));
        card.row(
                LookOverlay.outflow(
                        Component.translatable(
                                "overlay.hbm.tank_spaced",
                                NTMFluidProperties.getDisplayName(output.getFluid()),
                                NumberFormatter.formatGrouped(output.getFill()),
                                NumberFormatter.formatGrouped(output.getMaxFill()))));
    }
}
