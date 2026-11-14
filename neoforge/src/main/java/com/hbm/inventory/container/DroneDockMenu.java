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

import com.hbm.blockentity.network.DroneDockBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DroneDockMenu extends BlockEntityMenu<DroneDockBlockEntity> {

    public DroneDockMenu(int containerId, Inventory playerInventory, DroneDockBlockEntity dock) {
        super(ModMenus.DRONE_DOCK.get(), containerId, dock);
        checkContainerSize(dock, DroneDockBlockEntity.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(dock, col + row * 3, 62 + col * 18, 17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveFilteredUnsorted(player, index, DroneDockBlockEntity.SLOT_COUNT);
    }
}
