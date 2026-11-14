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

import com.hbm.blockentity.machine.SilexBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SILEXMenu extends BlockEntityMenu<SilexBlockEntity> {

    public SILEXMenu(int containerId, Inventory playerInventory, SilexBlockEntity blockEntity) {
        super(ModMenus.MACHINE_SILEX.get(), containerId, blockEntity);

        addSlot(new Slot(blockEntity, SilexBlockEntity.SLOT_INPUT, 80, 12));
        addSlot(new Slot(blockEntity, SilexBlockEntity.SLOT_FLUID_ID, 8, 24));
        addSlot(new Slot(blockEntity, SilexBlockEntity.SLOT_CONTAINER_IN, 26, 24));
        addSlot(
                new SlotFiltered(
                        blockEntity,
                        SilexBlockEntity.SLOT_CONTAINER_OUT,
                        44,
                        24,
                        SlotFiltered.NONE));
        addSlot(
                new SlotRecipeOutput(
                        playerInventory.player,
                        blockEntity,
                        SilexBlockEntity.SLOT_OUTPUT,
                        116,
                        90));
        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 2; column++) {
                int index = SilexBlockEntity.QUEUE_START + row * 2 + column;
                int x = 134 + column * 18;
                int y = 72 + row * 18;
                addSlot(new SlotRecipeOutput(playerInventory.player, blockEntity, index, x, y));
            }
        }
        addStandardInventorySlots(playerInventory, 8, 140);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                true,
                stack -> {
                    if (slotIndex < SilexBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, SilexBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (stack.getItem() instanceof FluidIdentifierItem) {
            return moveItemStackTo(
                    stack,
                    SilexBlockEntity.SLOT_FLUID_ID,
                    SilexBlockEntity.SLOT_FLUID_ID + 1,
                    false);
        }
        if (blockEntity().reactionTank().containerContent(stack) > 0) {
            return moveItemStackTo(
                    stack,
                    SilexBlockEntity.SLOT_CONTAINER_IN,
                    SilexBlockEntity.SLOT_CONTAINER_IN + 1,
                    false);
        }
        return moveItemStackTo(
                stack, SilexBlockEntity.SLOT_INPUT, SilexBlockEntity.SLOT_INPUT + 1, false);
    }
}
