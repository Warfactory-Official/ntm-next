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

package com.hbm.inventory.container;

import com.hbm.blockentity.network.pneumatic.PneumoStorageMonoBlockEntity;
import com.hbm.inventory.slot.SlotPattern;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PneumoStorageMonoMenu extends BlockEntityMenu<PneumoStorageMonoBlockEntity> {

    public PneumoStorageMonoMenu(
            int containerId, Inventory playerInventory, PneumoStorageMonoBlockEntity be) {
        super(ModMenus.PNEUMATIC_STORAGE_MONO.get(), containerId, be);
        checkContainerSize(be, PneumoStorageMonoBlockEntity.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            addSlot(new SlotPattern(be, i, 8, 17 + i * 18));
        }

        addStandardInventorySlots(playerInventory, 8, 99);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= PneumoStorageMonoBlockEntity.SLOT_COUNT) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        Slot slot = getSlot(slotIndex);
        if (blockEntity().amounts[slotIndex] > 0 && slot.hasItem()
                || !blockEntity().admitsFilter(getCarried())) return;

        slot.set(getCarried());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
