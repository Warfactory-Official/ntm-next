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

import com.hbm.blockentity.machine.ICFBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ICFMenu extends BlockEntityMenu<ICFBlockEntity> {

    public ICFMenu(int containerId, Inventory playerInventory, ICFBlockEntity machine) {
        super(ModMenus.MACHINE_ICF.get(), containerId, machine);

        for (var column = 0; column < 5; column++) {
            var x = 80 + column * 18;
            addSlot(new Slot(machine, ICFBlockEntity.SLOT_MAGAZINE_START + column, x, 18));
        }
        addSlot(new Slot(machine, ICFBlockEntity.SLOT_LOADED, 116, 54));
        for (var column = 0; column < 5; column++) {
            var x = 80 + column * 18;
            addSlot(
                    new SlotRecipeOutput(
                            playerInventory.player,
                            machine,
                            ICFBlockEntity.SLOT_CATCH_START + column,
                            x,
                            90));
        }
        addSlot(new Slot(machine, ICFBlockEntity.SLOT_FLUID_ID, 44, 90));

        addStandardInventorySlots(playerInventory, 44, 140);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                true,
                stack -> {
                    if (slotIndex < ICFBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, ICFBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    if (stack.getItem() instanceof FluidIdentifierItem) {
                        return moveItemStackTo(
                                stack,
                                ICFBlockEntity.SLOT_FLUID_ID,
                                ICFBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    }
                    if (moveItemStackTo(
                            stack,
                            ICFBlockEntity.SLOT_LOADED,
                            ICFBlockEntity.SLOT_LOADED + 1,
                            false)) {
                        return true;
                    }
                    return moveItemStackTo(
                            stack,
                            ICFBlockEntity.SLOT_MAGAZINE_START,
                            ICFBlockEntity.SLOT_MAGAZINE_END + 1,
                            false);
                });
    }
}
