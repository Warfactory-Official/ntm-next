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

import com.hbm.blockentity.machine.storage.MassStorageBlockEntity;
import com.hbm.blocks.PersistentItemTooltipProvider;
import com.hbm.blocks.machine.HorizontalMachineBlock;
import com.hbm.inventory.BlockMenus;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.tool.LockItem;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MassStorageBlock extends HorizontalMachineBlock
        implements PersistentItemTooltipProvider {

    public final int capacity;

    public MassStorageBlock(Properties props, int capacity) {
        super(props);
        this.capacity = capacity;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MassStorageBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.getItem() instanceof LockItem || stack.is(ModItems.KEY_KIT.get())) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage
                && storage.canAccess(player)) {
            BlockMenus.open(player, storage);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {}

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container == null) return;
        NonNullList<ItemStack> contents =
                NonNullList.withSize(MassStorageBlockEntity.SLOT_COUNT, ItemStack.EMPTY);
        container.copyInto(contents);
        ItemStack type = contents.get(MassStorageBlockEntity.SLOT_TYPE);
        if (type.isEmpty()) return;
        adder.accept(type.getHoverName().copy().withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.literal(
                        String.format(
                                Locale.ROOT,
                                "%,d / %,d",
                                stack.getOrDefault(ModDataComponents.MASS_STOCKPILE.get(), 0),
                                capacity)));
    }
}
