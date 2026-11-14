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
import com.hbm.blockentity.machine.thermal.CompactTurbineBlockEntity;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TurbineMenu extends BlockEntityMenu<CompactTurbineBlockEntity> {

    public TurbineMenu(int containerId, Inventory playerInv, CompactTurbineBlockEntity be) {
        super(ModMenus.MACHINE_TURBINE.get(), containerId, be);
        checkContainerSize(be, CompactTurbineBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        CompactTurbineBlockEntity.SLOT_FLUID_ID_IN,
                        8,
                        17,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));
        addSlot(
                new SlotFiltered(
                        be, CompactTurbineBlockEntity.SLOT_FLUID_ID_OUT, 8, 53, SlotFiltered.NONE));
        addSlot(new Slot(be, CompactTurbineBlockEntity.SLOT_CONTAINER_IN, 44, 17));
        addSlot(
                new SlotFiltered(
                        be,
                        CompactTurbineBlockEntity.SLOT_CONTAINER_OUT,
                        44,
                        53,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        be, CompactTurbineBlockEntity.SLOT_BATTERY, 98, 53, EnergyItem::matches));
        addSlot(new Slot(be, CompactTurbineBlockEntity.SLOT_EMPTY_CONTAINER_IN, 152, 17));
        addSlot(
                new SlotFiltered(
                        be,
                        CompactTurbineBlockEntity.SLOT_FILLED_CONTAINER_OUT,
                        152,
                        53,
                        SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return FluidTankNTM.isFluidContainer(stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = CompactTurbineBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                CompactTurbineBlockEntity.SLOT_FLUID_ID_IN,
                                CompactTurbineBlockEntity.SLOT_FLUID_ID_IN + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                CompactTurbineBlockEntity.SLOT_BATTERY,
                                CompactTurbineBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (isFluidContainer(stack))
                        return moveItemStackTo(
                                        stack,
                                        CompactTurbineBlockEntity.SLOT_CONTAINER_IN,
                                        CompactTurbineBlockEntity.SLOT_CONTAINER_IN + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        CompactTurbineBlockEntity.SLOT_EMPTY_CONTAINER_IN,
                                        CompactTurbineBlockEntity.SLOT_EMPTY_CONTAINER_IN + 1,
                                        false);
                    return false;
                });
    }
}
