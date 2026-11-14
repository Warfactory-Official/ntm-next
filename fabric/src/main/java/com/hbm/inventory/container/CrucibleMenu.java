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

import com.hbm.blockentity.machine.CrucibleBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CrucibleMenu extends BlockEntityMenu<CrucibleBlockEntity> {

    public CrucibleMenu(int containerId, Inventory playerInv, CrucibleBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private CrucibleMenu(
            int containerId, Inventory playerInv, Container container, CrucibleBlockEntity be) {
        super(ModMenus.MACHINE_CRUCIBLE.get(), containerId, be, container);
        checkContainerSize(container, CrucibleBlockEntity.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(
                        new Slot(container, j + i * 3 + 1, 107 + j * 18, 18 + i * 18) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return container.canPlaceItem(getContainerSlot(), stack);
                            }
                        });
            }
        }

        addStandardInventorySlots(playerInv, 8, 132);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, CrucibleBlockEntity.SLOT_COUNT);
    }
}
