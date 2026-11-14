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

import com.hbm.blockentity.machine.ReactorZirnoxBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.ZirnoxRodItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ReactorZirnoxMenu extends BlockEntityMenu<ReactorZirnoxBlockEntity> {
    private static final int ROD_SLOT_COUNT = 24;
    private static final int MACHINE_SLOT_COUNT = 28;

    public ReactorZirnoxMenu(
            int containerId, Inventory playerInventory, ReactorZirnoxBlockEntity machine) {
        super(ModMenus.REACTOR_ZIRNOX.get(), containerId, machine);

        addSlot(new Slot(machine, 0, 26, 16));
        addSlot(new Slot(machine, 1, 62, 16));
        addSlot(new Slot(machine, 2, 98, 16));
        addSlot(new Slot(machine, 3, 8, 34));
        addSlot(new Slot(machine, 4, 44, 34));
        addSlot(new Slot(machine, 5, 80, 34));
        addSlot(new Slot(machine, 6, 116, 34));
        addSlot(new Slot(machine, 7, 26, 52));
        addSlot(new Slot(machine, 8, 62, 52));
        addSlot(new Slot(machine, 9, 98, 52));
        addSlot(new Slot(machine, 10, 8, 70));
        addSlot(new Slot(machine, 11, 44, 70));
        addSlot(new Slot(machine, 12, 80, 70));
        addSlot(new Slot(machine, 13, 116, 70));
        addSlot(new Slot(machine, 14, 26, 88));
        addSlot(new Slot(machine, 15, 62, 88));
        addSlot(new Slot(machine, 16, 98, 88));
        addSlot(new Slot(machine, 17, 8, 106));
        addSlot(new Slot(machine, 18, 44, 106));
        addSlot(new Slot(machine, 19, 80, 106));
        addSlot(new Slot(machine, 20, 116, 106));
        addSlot(new Slot(machine, 21, 26, 124));
        addSlot(new Slot(machine, 22, 62, 124));
        addSlot(new Slot(machine, 23, 98, 124));
        addSlot(new Slot(machine, 24, 143, 124));
        addSlot(new SlotFiltered(machine, 26, 143, 142, SlotFiltered.NONE));
        addSlot(new Slot(machine, 25, 179, 124));
        addSlot(new SlotFiltered(machine, 27, 179, 142, SlotFiltered.NONE));
        addStandardInventorySlots(playerInventory, 8, 174);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < MACHINE_SLOT_COUNT) {
                        return moveItemStackTo(stack, MACHINE_SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        var machine = blockEntity();
        if (machine.carbonDioxide.containerContent(stack) > 0) {
            return moveItemStackTo(stack, 24, 25, false);
        }
        if (machine.water.containerContent(stack) > 0) {
            return moveItemStackTo(stack, 26, 27, false);
        }
        if (stack.getItem() instanceof ZirnoxRodItem) {
            return moveItemStackTo(stack, 0, ROD_SLOT_COUNT, true);
        }
        return false;
    }
}
