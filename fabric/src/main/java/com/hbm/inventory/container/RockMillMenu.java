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
import com.hbm.blockentity.machine.RockMillBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.BlueprintsItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RockMillMenu extends BlockEntityMenu<RockMillBlockEntity> {
    private final SyncedData data;

    public RockMillMenu(int containerId, Inventory playerInventory, RockMillBlockEntity be) {
        this(
                containerId,
                playerInventory,
                be,
                playerInventory.player.level().isClientSide()
                        ? SyncedData.client(RockMillBlockEntity.class)
                        : SyncedData.of(be));
    }

    private RockMillMenu(
            int containerId, Inventory playerInventory, RockMillBlockEntity be, SyncedData data) {
        super(ModMenus.MACHINE_ROCK_MILL.get(), containerId, be, be);
        checkContainerSize(be, RockMillBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        be, RockMillBlockEntity.SLOT_BATTERY, 152, 91, EnergyItem::matches));
        addSlot(
                new SlotFiltered(
                        be,
                        RockMillBlockEntity.SLOT_SCHEMATIC,
                        35,
                        90,
                        stack -> stack.getItem() instanceof BlueprintsItem));
        for (int i = 0; i < 3; i++) {
            addSlot(
                    SlotFiltered.gated(
                            be, RockMillBlockEntity.SLOT_INPUT_START + i, 8 + i * 18, 27));
        }
        for (int i = 0; i < 3; i++) {
            addSlot(
                    new SlotRecipeOutput(
                            playerInventory.player,
                            be,
                            RockMillBlockEntity.SLOT_OUTPUT_START + i,
                            80 + i * 18,
                            27));
        }
        addStandardInventorySlots(playerInventory, 8, 138);
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
                    int machineEnd = RockMillBlockEntity.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (EnergyItem.matches(stack)) return moveItemStackTo(stack, 0, 1, false);
                    if (stack.getItem() instanceof BlueprintsItem)
                        return moveItemStackTo(stack, 1, 2, false);
                    return moveItemStackTo(stack, 2, 5, false);
                });
    }
}
