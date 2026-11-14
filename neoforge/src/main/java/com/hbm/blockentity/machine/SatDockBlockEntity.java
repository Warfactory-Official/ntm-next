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
import com.hbm.inventory.container.SatDockMenu;
import com.hbm.items.ModItems;
import com.hbm.items.SatelliteFrequencyProvider;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class SatDockBlockEntity extends MachineBlockEntity implements MenuProvider {

    public static final int CARGO_SLOTS = 15;
    public static final int SLOT_CHIP = 15;
    public static final int SLOT_COUNT = 16;
    public static final int CARGO_REQUEST_PERIOD = 20;
    private static final int[] ACCESSIBLE_SLOTS = accessibleSlots();

    public SatDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINER_DOCK.get(), pos, state, SLOT_COUNT);
    }

    private static int[] accessibleSlots() {
        int[] slots = new int[CARGO_SLOTS];
        for (int i = 0; i < CARGO_SLOTS; i++) slots[i] = i;
        return slots;
    }

    @Override
    public void tickServer() {
        if (!TickPhase.every(this, CARGO_REQUEST_PERIOD)
                || !getItem(SLOT_CHIP).is(ModItems.SAT_CHIP.get())) return;
        ServerLevel server = (ServerLevel) level;
        BlockPos pos = getBlockPos();
        var frequency = SatelliteFrequencyProvider.frequency(getItem(SLOT_CHIP));
        var data = SatelliteSavedData.at(server, pos.getX(), pos.getZ(), frequency);
        var satellite = data.getSatFromFreq(frequency);
        if (satellite == null) return;
        satellite.requestPod(server, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_CHIP;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.satDock");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SatDockMenu(containerId, inventory, this);
    }
}
