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

import com.hbm.blockentity.machine.HeaterOilburnerBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.util.NumberFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HeaterOilburnerBlock extends MultiblockCoreBlock
        implements TickingEntityBlock,
                CapabilityBlock,
                ToolInteractionHandler,
                LookOverlayProvider {
    public HeaterOilburnerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeaterOilburnerBlockEntity(pos, state);
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER || player != null && player.isSpectator()) {
            return false;
        }
        if (!level.isClientSide()) {
            var heater = (HeaterOilburnerBlockEntity) level.getBlockEntity(pos);
            heater.toggleSetting();
            heater.setChanged();
            heater.networkPackNT(25);
        }
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay card) {
        var heater = (HeaterOilburnerBlockEntity) level.getBlockEntity(pos);
        card.title(getName());
        card.row(
                LookOverlay.inflow(
                        Component.translatable(
                                "unit.millibuckets_per_tick_spaced", heater.setting)));
        var fuel = NTMFluidProperties.getTrait(heater.tank.getTankType(), FlammableTrait.class);
        if (fuel != null) {
            int heatOutput = (int) (fuel.heatPerBucket() * heater.setting / 1000L);
            card.row(
                    LookOverlay.outflow(
                            Component.translatable(
                                    "unit.heat_per_tick_spaced",
                                    NumberFormatter.formatGrouped(heatOutput))));
        }
    }
}
