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

import com.hbm.blockentity.ordnance.launch.LaunchTableBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LaunchTableMenu extends BlockEntityMenu<LaunchTableBlockEntity> {

    private static final int SLOTS = LaunchTableBlockEntity.INVENTORY_SIZE;

    public LaunchTableMenu(int containerId, Inventory playerInv, LaunchTableBlockEntity table) {
        this(containerId, playerInv, table, table);
    }

    private LaunchTableMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            LaunchTableBlockEntity table) {
        super(ModMenus.LAUNCH_TABLE.get(), containerId, table, container);
        checkContainerSize(container, SLOTS);

        addSlot(new Slot(container, LaunchTableBlockEntity.MISSILE_SLOT, 26, 36));
        addSlot(new Slot(container, LaunchTableBlockEntity.DESIGNATOR_SLOT, 26, 72));
        addSlot(new Slot(container, LaunchTableBlockEntity.PROPELLANT_INPUT_SLOT, 116, 72));
        addSlot(new Slot(container, LaunchTableBlockEntity.OXIDIZER_INPUT_SLOT, 134, 72));
        addSlot(new Slot(container, LaunchTableBlockEntity.SOLID_INPUT_SLOT, 152, 90));
        addSlot(new Slot(container, LaunchTableBlockEntity.BATTERY_SLOT, 116, 108));
        addSlot(new Slot(container, LaunchTableBlockEntity.PROPELLANT_OUTPUT_SLOT, 116, 90));
        addSlot(new Slot(container, LaunchTableBlockEntity.OXIDIZER_OUTPUT_SLOT, 134, 90));

        addStandardInventorySlots(playerInv, 8, 140);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(blockEntity(), player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, SLOTS);
    }
}
