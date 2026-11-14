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

import com.hbm.blockentity.machine.BlastFurnaceBlockEntity;
import com.hbm.handler.FuelHandler;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BlastFurnaceMenu extends BlockEntityMenu<BlastFurnaceBlockEntity> {

    public BlastFurnaceMenu(int containerId, Inventory playerInv, BlastFurnaceBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private BlastFurnaceMenu(
            int containerId, Inventory playerInv, Container container, BlastFurnaceBlockEntity be) {
        super(ModMenus.MACHINE_BLAST_FURNACE.get(), containerId, be, container);
        checkContainerSize(container, BlastFurnaceBlockEntity.SLOT_COUNT);

        addSlot(SlotFiltered.gated(container, BlastFurnaceBlockEntity.SLOT_FUEL, 80, 81));
        addSlot(new Slot(container, BlastFurnaceBlockEntity.SLOT_INPUT_1, 80, 27));
        addSlot(new Slot(container, BlastFurnaceBlockEntity.SLOT_INPUT_2, 80, 45));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlastFurnaceBlockEntity.SLOT_OUTPUT_1,
                        134,
                        72));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        BlastFurnaceBlockEntity.SLOT_OUTPUT_2,
                        134,
                        90));

        addStandardInventorySlots(playerInv, 8, 140);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BlastFurnaceBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd)
                        return moveItemStackToFiltered(stack, machineEnd, invEnd, true);
                    if (FuelHandler.getBurnTime(player.level(), stack) > 0)
                        return moveItemStackToFiltered(
                                stack,
                                BlastFurnaceBlockEntity.SLOT_FUEL,
                                BlastFurnaceBlockEntity.SLOT_FUEL + 1,
                                false);
                    return moveItemStackToFiltered(
                            stack,
                            BlastFurnaceBlockEntity.SLOT_INPUT_1,
                            BlastFurnaceBlockEntity.SLOT_INPUT_2 + 1,
                            false);
                });
    }
}
