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

import com.hbm.blockentity.machine.SpentFuelCoolingBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WasteDrumMenu extends NtmContainerMenu {

    private static final int[] SLOTS = {
        71, 21, 89, 21, 53, 39, 71, 39, 89, 39, 107, 39, 53, 57, 71, 57, 89, 57, 107, 57, 71, 75,
        89, 75
    };

    public WasteDrumMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(12));
    }

    public WasteDrumMenu(int containerId, Inventory playerInv, SpentFuelCoolingBlockEntity be) {
        this(containerId, playerInv, (Container) be);
    }

    private WasteDrumMenu(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.WASTE_DRUM.get(), containerId, container);
        checkContainerSize(container, 12);

        for (int i = 0; i < 12; i++) {
            addSlot(
                    new Slot(container, i, SLOTS[i * 2], SLOTS[i * 2 + 1]) {
                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }
                    });
        }

        addStandardInventorySlots(playerInv, 8, 107);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = 12;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, machineEnd, false);
                });
    }
}
