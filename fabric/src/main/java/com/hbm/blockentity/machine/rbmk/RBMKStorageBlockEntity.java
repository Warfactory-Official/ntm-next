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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.RBMKStorageMenu;
import com.hbm.items.machine.RBMKRodItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class RBMKStorageBlockEntity extends RBMKBaseBlockEntity
        implements CraneLoadable, MenuProvider, SyncUnitSchema {
    private static final int[] AUTOMATED_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    public RBMKStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_STORAGE.get(), pos, state, 12);
    }

    @Override
    public void tickServer() {
        if (TickPhase.every(this, 10)) {
            for (int destination = 0; destination < 11; destination++) {
                if (!getItem(destination).isEmpty() || getItem(destination + 1).isEmpty()) continue;
                var stack = removeItemNoUpdate(destination + 1);
                setItem(destination, stack);
            }
        }
        super.tickServer();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATED_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof RBMKRodItem;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public boolean acceptsLoad(ItemStack stack) {
        return getItem(11).isEmpty();
    }

    @Override
    public void takeLoad(ItemStack stack) {
        setItem(11, stack.copy());
    }

    @Override
    public boolean hasUnload() {
        return !getItem(0).isEmpty();
    }

    @Override
    public ItemStack peekUnload() {
        return getItem(0);
    }

    @Override
    public void takeUnload() {
        setItem(0, ItemStack.EMPTY);
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.STORAGE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkStorage");
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RBMKStorageMenu(id, inventory, this);
    }
}
