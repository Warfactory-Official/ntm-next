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

import com.hbm.blockentity.network.CraneRouterBlockEntity;
import com.hbm.inventory.slot.SlotPattern;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class CraneRouterMenu extends BlockEntityMenu<CraneRouterBlockEntity> {
    public CraneRouterMenu(
            int containerId, Inventory playerInventory, CraneRouterBlockEntity machine) {
        super(ModMenus.CRANE_ROUTER.get(), containerId, machine);
        checkContainerSize(machine, CraneRouterBlockEntity.SLOT_COUNT);

        for (var bank = 0; bank < CraneRouterBlockEntity.SIDES; bank++) {
            var bankX = 34 + bank / 3 * 98;
            var y = 17 + bank % 3 * 26;
            for (var column = 0; column < CraneRouterBlockEntity.FILTERS_PER_SIDE; column++) {
                var inventorySlot = bank * CraneRouterBlockEntity.FILTERS_PER_SIDE + column;
                var x = bankX + column * 18;
                addSlot(new SlotPattern(machine, inventorySlot, x, y));
            }
        }
        addStandardInventorySlots(playerInventory, 47, 119);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput action, Player player) {
        if (player.isSpectator()) {
            return;
        }
        if (slotIndex >= 0 && slotIndex < CraneRouterBlockEntity.SLOT_COUNT) {
            if (action == ContainerInput.QUICK_MOVE) {
                return;
            }

            var machine = blockEntity();
            var template = getSlot(slotIndex);
            if (action == ContainerInput.PICKUP && button == 1 && !template.getItem().isEmpty()) {
                machine.advanceFilterMode(slotIndex);
            } else {
                template.set(getCarried());
                machine.initPattern(template.getItem(), slotIndex);
            }
            return;
        }
        super.clicked(slotIndex, button, action, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
