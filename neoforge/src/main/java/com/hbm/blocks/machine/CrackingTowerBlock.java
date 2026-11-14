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

import com.hbm.blockentity.machine.oil.CrackingTowerBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class CrackingTowerBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {

    public CrackingTowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrackingTowerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack stack,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(stack.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof CrackingTowerBlockEntity tower)) {
            return InteractionResult.PASS;
        }

        var identifier =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        var selectedFluid = identifier.primary();
        if (selectedFluid == null || selectedFluid == Fluids.EMPTY) return InteractionResult.PASS;
        if (player.isSpectator()) return InteractionResult.PASS;

        tower.tanks[0].setTankTypeByIdentifier(selectedFluid);
        tower.setChanged();
        player.sendSystemMessage(
                Component.translatable(
                        "hbm.message.crackingTowerSet",
                        NTMFluidProperties.getDisplayName(selectedFluid)));
        return InteractionResult.CONSUME;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay card) {
        if (!(level.getBlockEntity(pos) instanceof CrackingTowerBlockEntity tower)) return;

        card.title(getName());
        for (int index = 0; index < CrackingTowerBlockEntity.TANK_COUNT; index++) {
            var tank = tower.tanks[index];
            var contents =
                    Component.translatable(
                            "overlay.hbm.tank",
                            NTMFluidProperties.clientName(tank.getFluid()),
                            tank.getFill(),
                            tank.getMaxFill());
            card.row(LookOverlay.flow(index < 2, contents));
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut();
    }
}
