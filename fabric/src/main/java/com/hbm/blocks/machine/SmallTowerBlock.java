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

import com.hbm.blockentity.machine.TowerSmallBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class SmallTowerBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {
    public SmallTowerBlock(Properties props) {
        super(props);
    }

    static void towerOverlay(
            Level level, LookOverlay info, Block block, FluidTankNTM in, FluidTankNTM out) {
        info.title(block.getName());
        CondenserBlock.vacuumWarning(level, info);
        info.row(
                LookOverlay.inflow(
                        Component.translatable(
                                "overlay.hbm.tank",
                                NTMFluidProperties.clientName(in.getFluid()),
                                in.getFill(),
                                in.getMaxFill())));
        info.row(
                LookOverlay.outflow(
                        Component.translatable(
                                "overlay.hbm.tank",
                                NTMFluidProperties.clientName(out.getFluid()),
                                out.getFill(),
                                out.getMaxFill())));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TowerSmallBlockEntity(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof TowerSmallBlockEntity be)) return;
        towerOverlay(level, info, this, be.input, be.output);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TOWER_SMALL).fluidIn().fluidOut();
    }
}
