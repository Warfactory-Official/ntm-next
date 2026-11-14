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

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.LiquefactorMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class LiquefactorBlockEntity extends PhaseConverterBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_COUNT = 4;

    private static final int[] FACE_SLOTS = {SLOT_INPUT};

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private @Nullable FluidStackNTM melt;

    public LiquefactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIQUEFACTOR.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineLiquefactor");
    }

    @Override
    boolean ready() {
        ItemStack input = inventory.get(SLOT_INPUT);
        melt = LiquefactionRecipes.INSTANCE.getOutput(input);
        if (melt == null) return false;
        if (tank.getFill() > 0 && tank.getTankType() != melt.type()) return false;
        return tank.getFill() + melt.amount() <= tank.getMaxFill();
    }

    @Override
    void convert() {
        melt.fillInto(tank);
        inventory.get(SLOT_INPUT).shrink(1);
    }

    @Override
    void push() {
        flush.provide((ServerLevel) level, this);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return tankArray;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tank, FlushFaces.own());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) return LiquefactionRecipes.INSTANCE.getOutput(stack) != null;
        return canPlaceSharedSlot(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return FACE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_INPUT && canPlaceItem(slot, stack);
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new LiquefactorMenu(containerId, playerInventory, this);
    }
}
