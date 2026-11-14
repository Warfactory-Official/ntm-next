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

import com.hbm.blockentity.machine.AnnihilatorBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AnnihilatorMenu extends BlockEntityMenu<AnnihilatorBlockEntity> {

    public AnnihilatorMenu(int containerId, Inventory playerInv, AnnihilatorBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private AnnihilatorMenu(
            int containerId, Inventory playerInv, Container container, AnnihilatorBlockEntity be) {
        super(ModMenus.MACHINE_ANNIHILATOR.get(), containerId, be, container);
        checkContainerSize(container, AnnihilatorBlockEntity.SLOT_COUNT);

        addSlot(new Slot(container, AnnihilatorBlockEntity.SLOT_TRASH, 17, 45));
        addSlot(
                new SlotFiltered(
                        container,
                        AnnihilatorBlockEntity.SLOT_FLUID_ID,
                        35,
                        45,
                        s -> s.getItem() instanceof FluidIdentifierItem));
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int index = AnnihilatorBlockEntity.SLOT_PAYOUT_START + col + row * 3;
                addSlot(
                        new SlotFiltered(
                                container, index, 80 + col * 18, 36 + row * 18, SlotFiltered.NONE));
            }
        }
        addSlot(new Slot(container, AnnihilatorBlockEntity.SLOT_MONITOR, 152, 18));
        addSlot(new Slot(container, AnnihilatorBlockEntity.SLOT_PAYOUT_REQUEST, 152, 62));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        AnnihilatorBlockEntity.SLOT_PAYOUT_RESULT,
                        152,
                        80));

        addStandardInventorySlots(playerInv, 8, 126);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = AnnihilatorBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                AnnihilatorBlockEntity.SLOT_FLUID_ID,
                                AnnihilatorBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            AnnihilatorBlockEntity.SLOT_TRASH,
                            AnnihilatorBlockEntity.SLOT_TRASH + 1,
                            false);
                });
    }
}
