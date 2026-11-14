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
import com.hbm.blockentity.machine.GasCentBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GasCentMenu extends BlockEntityMenu<GasCentBlockEntity> {
    private final SyncedData data;

    public GasCentMenu(int containerId, Inventory playerInventory, GasCentBlockEntity machine) {
        super(ModMenus.MACHINE_GASCENT.get(), containerId, machine);

        for (var row = 0; row < 2; row++) {
            var y = 53 + row * 18;
            for (var column = 0; column < 2; column++) {
                var inventorySlot = GasCentBlockEntity.SLOT_OUTPUT_START + row * 2 + column;
                var x = 71 + column * 18;
                addSlot(new SlotRecipeOutput(playerInventory.player, machine, inventorySlot, x, y));
            }
        }
        addSlot(new Slot(machine, GasCentBlockEntity.SLOT_BATTERY, 182, 71));
        addSlot(new Slot(machine, GasCentBlockEntity.SLOT_FLUID_ID, 91, 15));
        addSlot(new Slot(machine, GasCentBlockEntity.SLOT_UPGRADE, 69, 15));
        addStandardInventorySlots(playerInventory, 8, 122);

        if (playerInventory.player.level().isClientSide()) {
            data = SyncedData.client(GasCentBlockEntity.class);
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
                true,
                stack -> {
                    if (index < GasCentBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, GasCentBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (stack.getItem() instanceof FluidIdentifierItem) {
            return moveItemStackTo(
                    stack,
                    GasCentBlockEntity.SLOT_FLUID_ID,
                    GasCentBlockEntity.SLOT_FLUID_ID + 1,
                    false);
        }
        if (EnergyItem.matches(stack)) {
            return moveItemStackTo(
                    stack,
                    GasCentBlockEntity.SLOT_BATTERY,
                    GasCentBlockEntity.SLOT_BATTERY + 1,
                    false);
        }
        if (MachineUpgradeItem.isUpgrade(stack)) {
            return moveItemStackTo(
                    stack,
                    GasCentBlockEntity.SLOT_UPGRADE,
                    GasCentBlockEntity.SLOT_UPGRADE + 1,
                    false);
        }
        return false;
    }
}
