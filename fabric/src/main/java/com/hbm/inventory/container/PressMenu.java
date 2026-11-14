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

import com.hbm.blockentity.processing.BurnerPressBlockEntity;
import com.hbm.handler.FuelHandler;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.StampItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PressMenu extends BlockEntityMenu<BurnerPressBlockEntity> {

    public PressMenu(int containerId, Inventory playerInv, BurnerPressBlockEntity be) {
        super(ModMenus.MACHINE_PRESS.get(), containerId, be);
        checkContainerSize(be, BurnerPressBlockEntity.SLOT_COUNT);

        addSlot(new Slot(be, BurnerPressBlockEntity.SLOT_FUEL, 26, 53));
        addSlot(
                new SlotFiltered(
                        be,
                        BurnerPressBlockEntity.SLOT_STAMP,
                        80,
                        17,
                        stack -> stack.getItem() instanceof StampItem));
        addSlot(
                new SlotFiltered(
                        be,
                        BurnerPressBlockEntity.SLOT_INPUT,
                        80,
                        53,
                        stack -> !(stack.getItem() instanceof StampItem)));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, BurnerPressBlockEntity.SLOT_OUTPUT, 140, 35));

        for (int i = 0; i < BurnerPressBlockEntity.SLOT_STORAGE_COUNT; i++) {
            addSlot(new Slot(be, BurnerPressBlockEntity.SLOT_STORAGE_START + i, 8 + i * 18, 84));
        }

        addStandardInventorySlots(playerInv, 8, 132);
    }

    public int getSpeedPercent() {
        return blockEntity().speed * 100 / BurnerPressBlockEntity.MAX_SPEED;
    }

    public int getOperationsLeft() {
        return blockEntity().burnTime / BurnerPressBlockEntity.FUEL_PER_OP;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BurnerPressBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (FuelHandler.getBurnTime(player.level(), stack) > 0)
                        return moveItemStackTo(
                                        stack,
                                        BurnerPressBlockEntity.SLOT_FUEL,
                                        BurnerPressBlockEntity.SLOT_FUEL + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        BurnerPressBlockEntity.SLOT_STORAGE_START,
                                        machineEnd,
                                        false);
                    if (stack.getItem() instanceof StampItem)
                        return moveItemStackTo(
                                        stack,
                                        BurnerPressBlockEntity.SLOT_STAMP,
                                        BurnerPressBlockEntity.SLOT_STAMP + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        BurnerPressBlockEntity.SLOT_STORAGE_START,
                                        machineEnd,
                                        false);
                    return moveItemStackTo(
                                    stack,
                                    BurnerPressBlockEntity.SLOT_INPUT,
                                    BurnerPressBlockEntity.SLOT_INPUT + 1,
                                    false)
                            || moveItemStackTo(
                                    stack,
                                    BurnerPressBlockEntity.SLOT_STORAGE_START,
                                    machineEnd,
                                    false);
                });
    }
}
