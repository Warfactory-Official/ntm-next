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

package com.hbm.blockentity.machine.oil;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.SolidifierMenu;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SolidificationRecipe;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class SolidifierBlockEntity extends PhaseConverterBlockEntity {

    public static final int SLOT_OUTPUT = 0;
    public static final int SLOT_FLUID_ID = 4;
    public static final int SLOT_COUNT = 5;

    private static final int[] FACE_SLOTS = {SLOT_OUTPUT};

    private @Nullable SolidificationRecipe recipe;

    public SolidifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLIDIFIER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSolidifier");
    }

    @Override
    void prepare() {
        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) markChanged();
    }

    @Override
    boolean ready() {
        recipe = SolidificationRecipes.INSTANCE.getOutput(tank.getTankType());
        return recipe != null
                && tank.getFill() >= recipe.fillReq()
                && Outputs.fitsFixed(inventory, SLOT_OUTPUT, recipe.outputItems());
    }

    @Override
    void convert() {
        tank.setFill(tank.getFill() - recipe.fillReq());
        Outputs.insertFixed(inventory, SLOT_OUTPUT, recipe.outputItems());
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return tankArray;
    }

    @Override
    public @Nullable FluidTankNTM pasteTank() {
        return tank;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FLUID_ID) return stack.getItem() instanceof FluidIdentifierItem;
        return canPlaceSharedSlot(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return FACE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new SolidifierMenu(containerId, playerInventory, this);
    }
}
