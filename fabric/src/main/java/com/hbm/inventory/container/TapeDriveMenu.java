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

import com.hbm.blockentity.machine.TapeDriveBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.DriveItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TapeDriveMenu extends BlockEntityMenu<TapeDriveBlockEntity> {
    public TapeDriveMenu(int containerId, Inventory playerInventory, TapeDriveBlockEntity be) {
        super(ModMenus.MACHINE_TAPE_DRIVE.get(), containerId, be);
        checkContainerSize(be, TapeDriveBlockEntity.SLOT_COUNT);
        for (int i = 0; i < TapeDriveBlockEntity.SLOT_COUNT; i++) {
            addSlot(
                    new SlotFiltered(
                            be,
                            i,
                            35 + (i % 6) * 18,
                            27 + (i / 6) * 18,
                            stack -> stack.getItem() instanceof DriveItem));
        }
        addStandardInventorySlots(playerInventory, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack ->
                        index < TapeDriveBlockEntity.SLOT_COUNT
                                ? moveItemStackTo(
                                        stack, TapeDriveBlockEntity.SLOT_COUNT, slots.size(), true)
                                : moveItemStackTo(
                                        stack, 0, TapeDriveBlockEntity.SLOT_COUNT, false));
    }
}
