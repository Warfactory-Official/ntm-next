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

package com.hbm.blockentity.network.pneumatic;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.PneumoStorageImporterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class PneumaticImporterBlockEntity extends PneumaticMachineBaseBlockEntity {

    public static final int SLOT_COUNT = 9;
    private static final int[] INPUT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private final int[] retryDelays = new int[SLOT_COUNT];

    public PneumaticImporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_STORAGE_IMPORTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (!stack.isEmpty()) retryDelays[slot] = Math.max(retryDelays[slot], 1);
    }

    @Override
    public void tickServer() {
        super.tickServer();
        var network = net();
        if (network == null) return;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (retryDelays[slot] > 0) {
                retryDelays[slot]--;
                continue;
            }
            var stack = inventory.get(slot);
            if (stack.isEmpty()) continue;
            int stored =
                    (int) network.give((ServerLevel) level, worldPosition, stack, stack.getCount());
            if (stored == 0) retryDelays[slot] = 100;
            else removeItem(slot, stored);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pneumoStorageImporter");
    }

    @Override
    public PneumoStorageImporterMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new PneumoStorageImporterMenu(containerId, playerInventory, this);
    }
}
