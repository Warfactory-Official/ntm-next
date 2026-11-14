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
import com.hbm.blockentity.machine.TurbofanBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TurbofanMenu extends BlockEntityMenu<TurbofanBlockEntity> {

    public TurbofanMenu(int containerId, Inventory playerInv, TurbofanBlockEntity be) {
        super(ModMenus.MACHINE_TURBOFAN.get(), containerId, be);
        checkContainerSize(be, TurbofanBlockEntity.SLOT_COUNT);

        addSlot(new Slot(be, TurbofanBlockEntity.SLOT_FLUID_IN, 17, 17));
        addSlot(
                new SlotFiltered(
                        be, TurbofanBlockEntity.SLOT_CONTAINER_OUT, 17, 53, SlotFiltered.NONE));
        addSlot(new Slot(be, TurbofanBlockEntity.SLOT_UPGRADE, 98, 71));
        addSlot(new Slot(be, TurbofanBlockEntity.SLOT_BATTERY, 143, 71));
        addSlot(new Slot(be, TurbofanBlockEntity.SLOT_FLUID_ID, 44, 71));

        addStandardInventorySlots(playerInv, 8, 121);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = TurbofanBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                TurbofanBlockEntity.SLOT_FLUID_ID,
                                TurbofanBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                TurbofanBlockEntity.SLOT_BATTERY,
                                TurbofanBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof MachineUpgradeItem)
                        return moveItemStackTo(
                                stack,
                                TurbofanBlockEntity.SLOT_UPGRADE,
                                TurbofanBlockEntity.SLOT_UPGRADE + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            TurbofanBlockEntity.SLOT_FLUID_IN,
                            TurbofanBlockEntity.SLOT_FLUID_IN + 1,
                            false);
                });
    }
}
