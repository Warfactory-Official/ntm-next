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

import com.hbm.blockentity.machine.ContinuousCaster;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class StrandCasterMenu extends BlockEntityMenu<ContinuousCaster> {

    public StrandCasterMenu(int containerId, Inventory playerInv, ContinuousCaster be) {
        this(containerId, playerInv, be, be);
    }

    private StrandCasterMenu(
            int containerId, Inventory playerInv, Container container, ContinuousCaster be) {
        super(ModMenus.MACHINE_STRAND_CASTER.get(), containerId, be, container);
        checkContainerSize(container, ContinuousCaster.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container,
                        ContinuousCaster.SLOT_MOLD,
                        57,
                        62,
                        stack -> container.canPlaceItem(ContinuousCaster.SLOT_MOLD, stack)));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player,
                                container,
                                ContinuousCaster.SLOT_OUTPUT_START + i * 2 + j,
                                125 + j * 18,
                                26 + i * 18));
            }
        }

        addStandardInventorySlots(playerInv, 8, 132);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = ContinuousCaster.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    return moveItemStackTo(
                            stack,
                            ContinuousCaster.SLOT_OUTPUT_START,
                            ContinuousCaster.SLOT_OUTPUT_START + 1,
                            false);
                });
    }
}
