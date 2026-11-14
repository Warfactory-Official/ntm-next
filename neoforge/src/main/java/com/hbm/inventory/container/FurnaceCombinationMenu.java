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

import com.hbm.blockentity.machine.thermal.CombinationFurnaceBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotSmelting;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FurnaceCombinationMenu extends BlockEntityMenu<CombinationFurnaceBlockEntity> {

    public FurnaceCombinationMenu(
            int containerId, Inventory playerInv, CombinationFurnaceBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private FurnaceCombinationMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            CombinationFurnaceBlockEntity be) {
        super(ModMenus.FURNACE_COMBINATION.get(), containerId, be, container);
        checkContainerSize(container, CombinationFurnaceBlockEntity.SLOT_COUNT);

        addSlot(new Slot(container, CombinationFurnaceBlockEntity.SLOT_INPUT, 26, 36));
        addSlot(
                new SlotSmelting(
                        playerInv.player,
                        container,
                        CombinationFurnaceBlockEntity.SLOT_OUTPUT,
                        89,
                        36));
        addSlot(new Slot(container, 2, 136, 18));
        addSlot(new SlotRecipeOutput(playerInv.player, container, 3, 136, 54));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = CombinationFurnaceBlockEntity.SLOT_COUNT;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    return moveItemStackTo(
                            stack,
                            CombinationFurnaceBlockEntity.SLOT_INPUT,
                            CombinationFurnaceBlockEntity.SLOT_INPUT + 1,
                            false);
                });
    }
}
