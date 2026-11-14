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

import com.hbm.blockentity.machine.oil.FractionTowerBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class FractionTowerBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, LookOverlayProvider, CapabilityBlock {
    public FractionTowerBlock(Properties props) {
        super(props);
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
        if (player.isShiftKeyDown() || !(stack.getItem() instanceof FluidIdentifierItem))
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(core) instanceof FractionTowerBlockEntity frac)) {
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(core.below(3)) instanceof FractionTowerBlockEntity) {
            player.sendSystemMessage(Component.translatable("chat.fractioning.onlybottom"));
            return InteractionResult.SUCCESS;
        }

        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null || type == Fluids.EMPTY) return InteractionResult.PASS;

        frac.tanks[0].setTankTypeByIdentifier(type);
        frac.setChanged();
        player.sendSystemMessage(typeChanged(type));
        return InteractionResult.SUCCESS;
    }

    public static Component typeChanged(Fluid type) {
        return Component.translatable("desc.shared.changedTypeTo")
                .withStyle(ChatFormatting.YELLOW)
                .append(NTMFluidProperties.getDisplayName(type))
                .append("!");
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof FractionTowerBlockEntity frac)) return;
        info.title(getName());
        for (int i = 0; i < frac.tanks.length; i++) {
            FluidTankNTM tank = frac.tanks[i];
            if (tank.getTankType() == null) continue;
            String arrow = i == 0 ? "-> " : "<- ";
            int color = i == 0 ? 0x55FF55 : 0xFF5555;
            info.row(
                    Component.literal(arrow)
                            .append(
                                    Component.translatable(
                                            "overlay.hbm.tank_spaced_unit",
                                            NTMFluidProperties.clientName(tank.getFluid()),
                                            tank.getFill(),
                                            tank.getMaxFill())),
                    color);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FractionTowerBlockEntity(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FRACTION_TOWER).fluidIn().fluidOut().items();
    }
}
