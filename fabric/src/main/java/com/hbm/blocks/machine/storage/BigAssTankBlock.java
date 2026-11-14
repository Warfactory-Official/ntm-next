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

package com.hbm.blocks.machine.storage;

import com.hbm.blockentity.machine.storage.BigAssTankBlockEntity;
import com.hbm.blockentity.machine.storage.FluidBarrelBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.PersistentItemTooltipProvider;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import java.util.function.Consumer;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public class BigAssTankBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, CapabilityBlock, PersistentItemTooltipProvider {

    public BigAssTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean tilts() {
        return true;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected boolean cellsHaveAnalogOutput() {
        return true;
    }

    @Override
    protected boolean comparatorCellAt(int localX, int localY, int localZ) {
        return localY == 0 && localX == 0 && Math.abs(localZ) == 6;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BigAssTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        var tank = (BigAssTankBlockEntity) level.getBlockEntity(core);
        openCoreMenu(player, core, tank);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(held.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        var identifier =
                held.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        var fluid = identifier.primary();
        if (fluid == null || fluid == Fluids.EMPTY) return InteractionResult.PASS;

        var tank = (BigAssTankBlockEntity) level.getBlockEntity(core);
        tank.tank.setTankTypeByIdentifier(fluid);
        tank.setChanged();
        player.sendSystemMessage(
                Component.translatable(
                                "desc.bigass.changed_type",
                                NTMFluidProperties.getDisplayName(fluid))
                        .withStyle(ChatFormatting.YELLOW));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        var content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        if (content == null) return;
        adder.accept(
                PersistentItemTooltipProvider.tankLine(content, BigAssTankBlockEntity.CAPACITY));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.BIGASSTANK)
                .fluidIn()
                .fluidOut()
                .items()
                .fluidFaces(
                        FluidBarrelBlockEntity.class,
                        (tank, face) -> face.fluid() == null || tank.connectsFluid(face.fluid()));
    }
}
