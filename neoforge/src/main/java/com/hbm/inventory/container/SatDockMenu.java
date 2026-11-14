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

import com.hbm.blockentity.machine.SatDockBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.SatelliteChipItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SatDockMenu extends BlockEntityMenu<SatDockBlockEntity> {

    private static final int SLOTS = SatDockBlockEntity.SLOT_COUNT;
    private static final int CARGO = SatDockBlockEntity.CARGO_SLOTS;
    private static final int COLUMNS = 5;

    public SatDockMenu(int containerId, Inventory playerInv, SatDockBlockEntity dock) {
        this(containerId, playerInv, dock, dock);
    }

    private SatDockMenu(
            int containerId, Inventory playerInv, Container container, SatDockBlockEntity dock) {
        super(ModMenus.SAT_DOCK.get(), containerId, dock, container);
        checkContainerSize(container, SLOTS);

        for (int i = 0; i < CARGO; i++) {
            addSlot(
                    new SlotFiltered(
                            container,
                            i,
                            71 + (i % COLUMNS) * 18,
                            18 + (i / COLUMNS) * 18,
                            SlotFiltered.NONE));
        }
        addSlot(
                new SlotFiltered(
                        container,
                        SatDockBlockEntity.SLOT_CHIP,
                        26,
                        36,
                        stack -> stack.getItem() instanceof SatelliteChipItem));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack ->
                        index < SLOTS
                                ? moveItemStackTo(stack, SLOTS, slots.size(), true)
                                : moveItemStackTo(stack, 0, CARGO, false));
    }
}
