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

import com.hbm.blockentity.machine.LaunchpadSoyuzBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LaunchpadSoyuzMenu extends BlockEntityMenu<LaunchpadSoyuzBlockEntity> {
    public LaunchpadSoyuzMenu(
            int containerId, Inventory playerInventory, LaunchpadSoyuzBlockEntity machine) {
        super(ModMenus.LAUNCHPAD_SOYUZ.get(), containerId, machine);

        addSlot(new PadSlot(machine, 0, 98, 80));
        addSlot(new PadSlot(machine, 1, 80, 80));
        addSlot(new PadSlot(machine, 2, 98, 26));
        addSlot(new PadSlot(machine, 3, 80, 26));
        addSlot(new Slot(machine, 4, 152, 98));
        addSlot(new Slot(machine, 5, 152, 116));
        addSlot(new Slot(machine, 6, 170, 98));
        addSlot(new Slot(machine, 7, 170, 116));
        addSlot(new Slot(machine, 8, 134, 98));
        for (var index = 9; index < LaunchpadSoyuzBlockEntity.SLOT_COUNT; index++) {
            var cargoIndex = index - 9;
            var x = 44 + cargoIndex / 6 * -18;
            var y = 26 + cargoIndex % 6 * 18;
            addSlot(new PadSlot(machine, index, x, y));
        }
        addStandardInventorySlots(playerInventory, 17, 162);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < LaunchpadSoyuzBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, LaunchpadSoyuzBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveItemStackTo(stack, 0, LaunchpadSoyuzBlockEntity.SLOT_COUNT, false);
                });
    }

    private static final class PadSlot extends Slot {
        private PadSlot(LaunchpadSoyuzBlockEntity machine, int index, int x, int y) {
            super(machine, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            var held = getItem();
            var heldCount = held.isEmpty() ? 1 : held.getCount();
            return Math.max(container.getMaxStackSize(), heldCount);
        }
    }
}
