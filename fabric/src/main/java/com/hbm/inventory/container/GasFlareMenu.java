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
import com.hbm.blockentity.machine.oil.GasFlareBlockEntity;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GasFlareMenu extends BlockEntityMenu<GasFlareBlockEntity> {

    public GasFlareMenu(int containerId, Inventory playerInv, GasFlareBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private GasFlareMenu(
            int containerId, Inventory playerInv, Container container, GasFlareBlockEntity be) {
        super(ModMenus.MACHINE_GAS_FLARE.get(), containerId, be, container);
        checkContainerSize(container, GasFlareBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container, GasFlareBlockEntity.SLOT_BATTERY, 143, 71, EnergyItem::matches));
        addSlot(new Slot(container, GasFlareBlockEntity.SLOT_FLUID_IN, 17, 17));
        addSlot(
                new SlotFiltered(
                        container, GasFlareBlockEntity.SLOT_FLUID_OUT, 17, 53, SlotFiltered.NONE));
        addSlot(new Slot(container, GasFlareBlockEntity.SLOT_FLUID_ID, 35, 71));
        addSlot(new SlotUpgrade(container, GasFlareBlockEntity.SLOT_UPGRADE_START, 80, 71));
        addSlot(new SlotUpgrade(container, GasFlareBlockEntity.SLOT_UPGRADE_END, 98, 71));

        addStandardInventorySlots(playerInv, 8, 121);
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
                    int machineEnd = GasFlareBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                GasFlareBlockEntity.SLOT_FLUID_ID,
                                GasFlareBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                GasFlareBlockEntity.SLOT_BATTERY,
                                GasFlareBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                GasFlareBlockEntity.SLOT_UPGRADE_START,
                                GasFlareBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    if (isFluidContainer(stack))
                        return moveItemStackTo(
                                stack,
                                GasFlareBlockEntity.SLOT_FLUID_IN,
                                GasFlareBlockEntity.SLOT_FLUID_IN + 1,
                                false);
                    return false;
                });
    }
}
