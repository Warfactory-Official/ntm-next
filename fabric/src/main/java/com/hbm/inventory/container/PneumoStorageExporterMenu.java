/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.inventory.container;

import com.hbm.blockentity.network.pneumatic.PneumoStorageExporterBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotPattern;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class PneumoStorageExporterMenu extends BlockEntityMenu<PneumoStorageExporterBlockEntity> {
    private static final int FILTER_SLOT_COUNT = 9;

    public PneumoStorageExporterMenu(
            int containerId, Inventory playerInventory, PneumoStorageExporterBlockEntity machine) {
        super(ModMenus.PNEUMATIC_STORAGE_EXPORTER.get(), containerId, machine);

        for (var index = 0; index < FILTER_SLOT_COUNT; index++) {
            var x = 17 + index % 3 * 18;
            var y = 17 + index / 3 * 18;
            addSlot(new SlotPattern(machine, index, x, y).allowStackSize());
        }
        for (var index = FILTER_SLOT_COUNT;
                index < PneumoStorageExporterBlockEntity.SLOT_COUNT;
                index++) {
            var outputIndex = index - FILTER_SLOT_COUNT;
            var x = 80 + outputIndex % 3 * 18;
            var y = 17 + outputIndex / 3 * 18;
            addSlot(new SlotFiltered(machine, index, x, y, SlotFiltered.NONE));
        }
        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput action, Player player) {
        if (slotIndex >= 0 && slotIndex < FILTER_SLOT_COUNT) {
            getSlot(slotIndex).set(getCarried());
            return;
        }
        super.clicked(slotIndex, button, action, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < FILTER_SLOT_COUNT || index >= PneumoStorageExporterBlockEntity.SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return quickMove(
                player,
                index,
                false,
                stack ->
                        moveItemStackTo(
                                stack,
                                PneumoStorageExporterBlockEntity.SLOT_COUNT,
                                slots.size(),
                                true));
    }
}
