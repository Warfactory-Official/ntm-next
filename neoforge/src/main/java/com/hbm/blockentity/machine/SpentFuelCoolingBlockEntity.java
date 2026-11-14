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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.WasteDrumMenu;
import com.hbm.inventory.recipes.FuelPoolRecipes;
import com.hbm.items.machine.RBMKRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SpentFuelCoolingBlockEntity extends MachineBlockEntity implements MenuProvider {
    private static final int[] EXPOSED_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private int waterFaces;
    private boolean waterFacesComplete;

    public SpentFuelCoolingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASTE_DRUM.get(), pos, state, 12);
    }

    @Override
    public void tickServer() {
        if (!waterFacesComplete) refreshWaterFaces();
        if (waterFaces == 0) return;
        int interval = 72000 / waterFaces;
        for (int slot = 0; slot < 12; slot++) {
            var stack = inventory.get(slot);
            beforeCoolingStack(stack, interval);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof RBMKRodItem rod) {
                double core = RBMKRodItem.getCoreHeat(stack);
                double hull = RBMKRodItem.getHullHeat(stack);
                rod.updateHeat(level, stack, 0.025D);
                rod.provideHeat(level, stack, 20D, 0.025D);
                if (core != RBMKRodItem.getCoreHeat(stack)
                        || hull != RBMKRodItem.getHullHeat(stack)) setChanged();
            } else if (level.getRandom().nextInt(interval) == 0) {
                var recipe = FuelPoolRecipes.getRecipe(stack);
                if (recipe != null) {
                    inventory.set(slot, recipe.output().copy());
                    setChanged();
                }
            }
        }
    }

    protected void beforeCoolingStack(ItemStack stack, int interval) {}

    public void refreshWaterFaces() {
        int count = 0;
        boolean complete = true;
        for (var direction : Direction.VALUES) {
            var neighbour = worldPosition.relative(direction);
            if (level.hasChunkAt(neighbour)) {
                if (level.getBlockState(neighbour).is(Blocks.WATER)) count++;
            } else {
                complete = false;
            }
        }
        waterFaces = count;
        waterFacesComplete = complete;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return EXPOSED_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof RBMKRodItem || FuelPoolRecipes.getRecipe(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return stack.getItem() instanceof RBMKRodItem
                ? RBMKRodItem.getCoreHeat(stack) < 50D && RBMKRodItem.getHullHeat(stack) < 50D
                : FuelPoolRecipes.getRecipe(stack) == null;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.wasteDrum");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WasteDrumMenu(id, inventory, this);
    }
}
