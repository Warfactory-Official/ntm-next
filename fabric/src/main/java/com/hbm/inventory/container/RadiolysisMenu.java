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

import com.hbm.blockentity.machine.RadiolysisBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RadiolysisMenu extends BlockEntityMenu<RadiolysisBlockEntity> {
    private final SyncedData data;

    public RadiolysisMenu(
            int containerId, Inventory playerInventory, RadiolysisBlockEntity machine) {
        super(ModMenus.MACHINE_RADIOLYSIS.get(), containerId, machine);

        for (var column = 0; column < 2; column++) {
            var x = 188 + column * 18;
            for (var row = 0; row < 5; row++) {
                var inventorySlot = column * 5 + row;
                var y = 8 + row * 18;
                addSlot(new Slot(machine, inventorySlot, x, y));
            }
        }
        addSlot(new Slot(machine, RadiolysisBlockEntity.IDENTIFIER_INPUT, 34, 17));
        addSlot(
                new SlotFiltered(
                        machine,
                        RadiolysisBlockEntity.IDENTIFIER_OUTPUT,
                        34,
                        53,
                        SlotFiltered.NONE));
        addSlot(new Slot(machine, RadiolysisBlockEntity.TREATMENT_INPUT, 148, 17));
        addSlot(
                new SlotFiltered(
                        machine,
                        RadiolysisBlockEntity.TREATMENT_OUTPUT,
                        148,
                        53,
                        SlotFiltered.NONE));
        addSlot(new Slot(machine, RadiolysisBlockEntity.BATTERY_SLOT, 8, 53));
        addStandardInventorySlots(playerInventory, 8, 84);

        if (playerInventory.player.level().isClientSide()) {
            data = SyncedData.client(RadiolysisBlockEntity.class);
        } else {
            data = SyncedData.of(machine);
        }
        addDataSlots(data);
    }

    public int getHeat() {
        return data.getInt("heat");
    }

    public long getPower() {
        return blockEntity().getPower();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < RadiolysisBlockEntity.INVENTORY_SIZE) {
                        return moveItemStackTo(
                                stack, RadiolysisBlockEntity.INVENTORY_SIZE, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (stack.getItem() instanceof FluidIdentifierItem) {
            var moved =
                    moveItemStackTo(
                            stack,
                            RadiolysisBlockEntity.IDENTIFIER_INPUT,
                            RadiolysisBlockEntity.IDENTIFIER_INPUT + 1,
                            false);
            if (moved) {
                return true;
            }
        }
        return moveItemStackTo(stack, 0, RadiolysisBlockEntity.INVENTORY_SIZE, false);
    }
}
