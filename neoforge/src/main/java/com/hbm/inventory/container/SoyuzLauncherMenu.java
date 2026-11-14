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

import com.hbm.blockentity.machine.SoyuzLauncherBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Deprecated
public class SoyuzLauncherMenu extends BlockEntityMenu<SoyuzLauncherBlockEntity> {
    public SoyuzLauncherMenu(
            int containerId, Inventory playerInventory, SoyuzLauncherBlockEntity machine) {
        super(ModMenus.SOYUZ_LAUNCHER.get(), containerId, machine);

        addSlot(new Slot(machine, SoyuzLauncherBlockEntity.SLOT_ROCKET, 98, 80));
        addSlot(new Slot(machine, SoyuzLauncherBlockEntity.SLOT_DESIGNATOR, 80, 80));
        addSlot(new Slot(machine, SoyuzLauncherBlockEntity.SLOT_SATELLITE, 98, 26));
        addSlot(new Slot(machine, SoyuzLauncherBlockEntity.SLOT_MODULE, 80, 26));
        addSlot(new Slot(machine, SoyuzLauncherBlockEntity.SLOT_FUEL_IN, 152, 98));
        addSlot(new Slot(machine, SoyuzLauncherBlockEntity.SLOT_FUEL_OUT, 152, 116));
        addSlot(new Slot(machine, SoyuzLauncherBlockEntity.SLOT_OXIDIZER_IN, 170, 98));
        addSlot(new Slot(machine, SoyuzLauncherBlockEntity.SLOT_OXIDIZER_OUT, 170, 116));
        addSlot(new Slot(machine, SoyuzLauncherBlockEntity.SLOT_BATTERY, 134, 98));
        for (var index = SoyuzLauncherBlockEntity.SLOT_CARGO;
                index < SoyuzLauncherBlockEntity.SLOT_COUNT;
                index++) {
            var cargoIndex = index - SoyuzLauncherBlockEntity.SLOT_CARGO;
            var x = 44 + cargoIndex / 6 * -18;
            var y = 26 + cargoIndex % 6 * 18;
            addSlot(new Slot(machine, index, x, y));
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
                    if (index < SoyuzLauncherBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, SoyuzLauncherBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveItemStackTo(
                            stack,
                            SoyuzLauncherBlockEntity.SLOT_ROCKET,
                            SoyuzLauncherBlockEntity.SLOT_ROCKET + 1,
                            false);
                });
    }
}
