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

import com.hbm.blockentity.machine.thermal.SteelFurnaceBlockEntity;
import com.hbm.inventory.slot.SlotSmelting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FurnaceSteelMenu extends BlockEntityMenu<SteelFurnaceBlockEntity> {

    public FurnaceSteelMenu(
            int containerId, Inventory playerInventory, SteelFurnaceBlockEntity machine) {
        super(ModMenus.FURNACE_STEEL.get(), containerId, machine);

        for (var lane = 0; lane < 3; lane++) {
            var y = 17 + lane * 18;
            addSlot(new Slot(machine, lane, 35, y));
        }
        for (var lane = 0; lane < 3; lane++) {
            var y = 17 + lane * 18;
            addSlot(new SlotSmelting(playerInventory.player, machine, 3 + lane, 125, y));
        }

        addStandardInventorySlots(playerInventory, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                true,
                stack -> {
                    if (slotIndex < SteelFurnaceBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, SteelFurnaceBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveItemStackTo(stack, 0, 3, false);
                });
    }
}
