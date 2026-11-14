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

import com.hbm.blockentity.generator.RadGenBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RadGenMenu extends BlockEntityMenu<RadGenBlockEntity> {

    private static final int COLUMNS = 3;
    private static final int ROWS = 4;

    public RadGenMenu(int containerId, Inventory playerInv, RadGenBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private RadGenMenu(
            int containerId, Inventory playerInv, Container container, RadGenBlockEntity be) {
        super(ModMenus.MACHINE_RADGEN.get(), containerId, be, container);
        checkContainerSize(container, RadGenBlockEntity.SLOT_COUNT);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int slot = col + row * COLUMNS;
                addSlot(
                        new SlotFiltered(
                                container,
                                slot,
                                8 + col * 18,
                                17 + row * 18,
                                stack -> be.canPlaceItem(slot, stack)));
            }
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player,
                                container,
                                col + row * COLUMNS + RadGenBlockEntity.CHANNELS,
                                116 + col * 18,
                                17 + row * 18));
            }
        }

        addStandardInventorySlots(playerInv, 8, 102);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index < RadGenBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, RadGenBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveItemStackTo(stack, 0, RadGenBlockEntity.CHANNELS, false);
                });
    }
}
