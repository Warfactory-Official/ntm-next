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

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.blockentity.processing.ElectrolysisBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ElectrolyserFluidMenu extends BlockEntityMenu<ElectrolysisBlockEntity> {
    private static final int MACHINE_SLOT_COUNT = 14;

    public ElectrolyserFluidMenu(
            int containerId, Inventory playerInventory, ElectrolysisBlockEntity machine) {
        super(ModMenus.MACHINE_ELECTROLYSER_FLUID.get(), containerId, machine);

        addSlot(new Slot(machine, 0, 186, 109));
        for (var row = 0; row < 2; row++) {
            var inventorySlot = 1 + row;
            var y = 140 + row * 18;
            addSlot(new Slot(machine, inventorySlot, 186, y));
        }

        addContainerLane(playerInventory.player, machine, 3, 6);
        addContainerLane(playerInventory.player, machine, 5, 24);
        addContainerLane(playerInventory.player, machine, 7, 78);
        addContainerLane(playerInventory.player, machine, 9, 134);
        for (var row = 0; row < 3; row++) {
            var inventorySlot = 11 + row;
            var y = 18 + row * 18;
            addSlot(new SlotRecipeOutput(playerInventory.player, machine, inventorySlot, 154, y));
        }

        addStandardInventorySlots(playerInventory, 8, 122);
    }

    private void addContainerLane(
            Player player, ElectrolysisBlockEntity machine, int inputSlot, int x) {
        addSlot(new Slot(machine, inputSlot, x, 18));
        addSlot(new SlotRecipeOutput(player, machine, inputSlot + 1, x, 54));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index < MACHINE_SLOT_COUNT) {
                        return moveItemStackTo(stack, MACHINE_SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (stack.getItem() instanceof FluidIdentifierItem) {
            return moveItemStackTo(stack, 3, 4, false);
        }
        if (EnergyItem.matches(stack)) {
            return moveItemStackTo(stack, 0, 1, false);
        }
        if (MachineUpgradeItem.isUpgrade(stack)) {
            return moveItemStackTo(stack, 1, 3, false);
        }
        return false;
    }
}
