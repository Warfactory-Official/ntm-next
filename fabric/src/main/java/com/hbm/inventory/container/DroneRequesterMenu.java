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

import com.hbm.blockentity.network.DroneRequesterBlockEntity;
import com.hbm.inventory.slot.SlotPattern;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DroneRequesterMenu extends BlockEntityMenu<DroneRequesterBlockEntity> {

    public DroneRequesterMenu(
            int containerId, Inventory playerInventory, DroneRequesterBlockEntity requester) {
        super(ModMenus.DRONE_REQUESTER.get(), containerId, requester);
        checkContainerSize(requester, DroneRequesterBlockEntity.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotPattern(requester, col + row * 3, 98 + col * 18, 17 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = col + row * 3 + DroneRequesterBlockEntity.FILTER_COUNT;
                addSlot(new Slot(requester, slot, 26 + col * 18, 17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < DroneRequesterBlockEntity.FILTER_COUNT) return ItemStack.EMPTY;

        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineSlots = DroneRequesterBlockEntity.SLOT_COUNT;
                    if (index < machineSlots)
                        return moveItemStackToFiltered(stack, machineSlots, slots.size(), true);
                    return moveItemStackToFiltered(
                            stack, DroneRequesterBlockEntity.FILTER_COUNT, machineSlots, false);
                });
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (player.isSpectator()) return;
        if (slotIndex < 0 || slotIndex >= DroneRequesterBlockEntity.FILTER_COUNT) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        DroneRequesterBlockEntity requester = blockEntity();
        Slot slot = getSlot(slotIndex);

        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            requester.advanceFilterMode(slotIndex);
            requester.setChanged();
            return;
        }

        ItemStack carried = getCarried();
        ItemStack pattern = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
        requester.setItem(slotIndex, pattern);
        requester.matcher.initPatternStandard(requester.getLevel(), pattern, slotIndex);
    }
}
