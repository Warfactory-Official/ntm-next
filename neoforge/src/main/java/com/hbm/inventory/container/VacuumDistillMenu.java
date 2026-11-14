/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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
import com.hbm.blockentity.machine.oil.VacuumRefineryBlockEntity;
import com.hbm.inventory.slot.SlotDeprecated;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class VacuumDistillMenu extends BlockEntityMenu<VacuumRefineryBlockEntity> {
    private final SyncedData data;

    public VacuumDistillMenu(
            int containerId, Inventory playerInventory, VacuumRefineryBlockEntity machine) {
        super(ModMenus.MACHINE_VACUUM_DISTILL.get(), containerId, machine);

        addSlot(new Slot(machine, VacuumRefineryBlockEntity.SLOT_BATTERY, 26, 90));
        addSlot(new SlotDeprecated(machine, VacuumRefineryBlockEntity.SLOT_FEED_IN, 44, 90));
        addSlot(new SlotDeprecated(machine, VacuumRefineryBlockEntity.SLOT_FEED_OUT, 44, 108));
        for (var column = 0; column < 4; column++) {
            var inputSlot = VacuumRefineryBlockEntity.SLOT_FEED_OUT + 1 + column * 2;
            var x = 80 + column * 18;
            addSlot(new Slot(machine, inputSlot, x, 90));
            addSlot(new SlotFiltered(machine, inputSlot + 1, x, 108, SlotFiltered.NONE));
        }
        addSlot(new Slot(machine, VacuumRefineryBlockEntity.SLOT_FLUID_ID, 26, 108));
        addStandardInventorySlots(playerInventory, 8, 156);

        if (playerInventory.player.level().isClientSide()) {
            data = SyncedData.client(VacuumRefineryBlockEntity.class);
        } else {
            data = SyncedData.of(machine);
        }
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < VacuumRefineryBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, VacuumRefineryBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (stack.getItem() instanceof FluidIdentifierItem) {
            var moved =
                    moveItemStackTo(
                            stack,
                            VacuumRefineryBlockEntity.SLOT_FLUID_ID,
                            VacuumRefineryBlockEntity.SLOT_FLUID_ID + 1,
                            false);
            if (moved) {
                return true;
            }
        }
        if (EnergyItem.matches(stack)) {
            return moveItemStackTo(
                    stack,
                    VacuumRefineryBlockEntity.SLOT_BATTERY,
                    VacuumRefineryBlockEntity.SLOT_BATTERY + 1,
                    false);
        }

        for (var inputSlot = VacuumRefineryBlockEntity.SLOT_FEED_OUT + 1;
                inputSlot < VacuumRefineryBlockEntity.SLOT_FLUID_ID;
                inputSlot += 2) {
            if (moveItemStackTo(stack, inputSlot, inputSlot + 1, false)) {
                return true;
            }
        }
        return moveItemStackTo(
                stack,
                VacuumRefineryBlockEntity.SLOT_FLUID_ID,
                VacuumRefineryBlockEntity.SLOT_FLUID_ID + 1,
                false);
    }
}
