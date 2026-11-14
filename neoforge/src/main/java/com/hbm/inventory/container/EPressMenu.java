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
import com.hbm.blockentity.processing.ElectricPressBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.StampItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class EPressMenu extends BlockEntityMenu<ElectricPressBlockEntity> {

    public EPressMenu(int containerId, Inventory playerInv, ElectricPressBlockEntity be) {
        super(ModMenus.MACHINE_EPRESS.get(), containerId, be);
        checkContainerSize(be, ElectricPressBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be, ElectricPressBlockEntity.SLOT_BATTERY, 152, 54, EnergyItem::matches));
        addSlot(
                new SlotFiltered(
                        be,
                        ElectricPressBlockEntity.SLOT_STAMP,
                        19,
                        15,
                        stack -> stack.getItem() instanceof StampItem));
        addSlot(
                new SlotFiltered(
                        be,
                        ElectricPressBlockEntity.SLOT_INPUT,
                        19,
                        51,
                        stack -> !(stack.getItem() instanceof StampItem)));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, ElectricPressBlockEntity.SLOT_OUTPUT, 79, 33));
        addSlot(new SlotUpgrade(be, ElectricPressBlockEntity.SLOT_UPGRADE, 111, 32));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = ElectricPressBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                ElectricPressBlockEntity.SLOT_BATTERY,
                                ElectricPressBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                ElectricPressBlockEntity.SLOT_UPGRADE,
                                ElectricPressBlockEntity.SLOT_UPGRADE + 1,
                                false);
                    if (stack.getItem() instanceof StampItem)
                        return moveItemStackTo(
                                stack,
                                ElectricPressBlockEntity.SLOT_STAMP,
                                ElectricPressBlockEntity.SLOT_STAMP + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            ElectricPressBlockEntity.SLOT_INPUT,
                            ElectricPressBlockEntity.SLOT_INPUT + 1,
                            false);
                });
    }
}
