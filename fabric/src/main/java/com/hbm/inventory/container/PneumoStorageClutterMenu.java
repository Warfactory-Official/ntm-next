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

import com.hbm.blockentity.network.pneumatic.PneumaticStorageClutterBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;

public class PneumoStorageClutterMenu extends BlockEntityMenu<PneumaticStorageClutterBlockEntity> {

    public PneumoStorageClutterMenu(
            int containerId, Inventory playerInventory, PneumaticStorageClutterBlockEntity be) {
        super(ModMenus.PNEUMATIC_STORAGE_CLUTTER.get(), containerId, be);
        checkContainerSize(be, PneumaticStorageClutterBlockEntity.SLOT_COUNT);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new ShulkerBoxSlot(be, col + row * 9, 8 + col * 18, 17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 153);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = PneumaticStorageClutterBlockEntity.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackToFiltered(stack, machineEnd, slots.size(), true);
                    return moveItemStackToFiltered(stack, 0, machineEnd, false) || false;
                });
    }
}
