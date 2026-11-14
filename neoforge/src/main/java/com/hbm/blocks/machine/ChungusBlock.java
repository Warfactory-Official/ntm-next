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

import com.hbm.blockentity.machine.thermal.LeviathanTurbineBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.CoolableTrait;
import com.hbm.util.NumberFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

public class ChungusBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock, LookOverlayProvider {

    public ChungusBlock(Properties properties) {
        super(properties);
    }

    @Override
    public LeviathanTurbineBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeviathanTurbineBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;

        var facing = state.getValue(FACING);
        var left = facing.getCounterClockWise();
        var clicked = hit.getBlockPos();
        int dx = clicked.getX() - core.getX();
        int dz = clicked.getZ() - core.getZ();
        int ahead = dx * facing.getStepX() + dz * facing.getStepZ();
        int lateral = dx * left.getStepX() + dz * left.getStepZ();
        if (clicked.getY() < core.getY() + 2 && lateral == 2 && (ahead == 1 || ahead == 2)) {
            var turbine = (LeviathanTurbineBlockEntity) level.getBlockEntity(core);
            return IndustrialTurbineBlock.pullLever(level, clicked, player, turbine);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        var owner = MultiblockCoreBlock.ownerOf(level, pos);
        if (owner == null) return;
        var turbine = (LeviathanTurbineBlockEntity) level.getBlockEntity(owner.pos());
        var input = turbine.tanks[0];
        var output = turbine.tanks[1];
        var cooling = NTMFluidProperties.getTrait(input.getTankType(), CoolableTrait.class);
        Fluid product = NTMFluids.NONE;
        if (cooling != null) product = cooling.product();

        info.title(getName());
        info.row(
                LookOverlay.inflow(
                        Component.translatable(
                                "overlay.hbm.tank",
                                NTMFluidProperties.getDisplayName(input.getFluid()),
                                NumberFormatter.formatGrouped(input.getFill()),
                                NumberFormatter.formatGrouped(input.getMaxFill()))));
        info.row(
                LookOverlay.outflow(
                        Component.translatable(
                                "overlay.hbm.tank",
                                NTMFluidProperties.getDisplayName(product),
                                NumberFormatter.formatGrouped(output.getFill()),
                                NumberFormatter.formatGrouped(output.getMaxFill()))));
        info.row(
                LookOverlay.outflow(
                        Component.translatable(
                                "unit.energy",
                                NumberFormatter.formatCompact(turbine.powerBuffer))));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed()
                .powerOut()
                .fluidIn()
                .fluidOut()
                .fe()
                .faces(
                        LeviathanTurbineBlockEntity.class,
                        (_, side) ->
                                side != Direction.UP && side != Direction.DOWN);
    }
}
