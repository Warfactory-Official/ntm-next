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

import com.hbm.blockentity.turret.TurretBaseNTBlockEntity;
import com.hbm.blockentity.turret.TurretFritzBlockEntity;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TurretBaseMenu extends BlockEntityMenu<TurretBaseNTBlockEntity> {
    private static final int FRITZ_SERVICE_MENU_SLOT = 9;

    private final boolean opened;

    public TurretBaseMenu(
            int containerId, Inventory playerInventory, TurretBaseNTBlockEntity machine) {
        super(ModMenus.TURRET_BASE.get(), containerId, machine);
        opened = !playerInventory.player.isSpectator();
        if (opened) {
            machine.startOpen(playerInventory.player);
        }

        addSlot(new Slot(machine, TurretBaseNTBlockEntity.SLOT_CHIP, 98, 27));
        for (var index = TurretBaseNTBlockEntity.SLOT_AMMO_FIRST;
                index <= TurretBaseNTBlockEntity.SLOT_AMMO_LAST;
                index++) {
            var ammoIndex = index - TurretBaseNTBlockEntity.SLOT_AMMO_FIRST;
            var x = 80 + ammoIndex % 3 * 18;
            var y = 63 + ammoIndex / 3 * 18;
            addSlot(new Slot(machine, index, x, y));
        }
        addSlot(new Slot(machine, TurretBaseNTBlockEntity.SLOT_BATTERY, 152, 99));
        addStandardInventorySlots(playerInventory, 8, 140);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity().stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (opened) {
            blockEntity().stopOpen(player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index < TurretBaseNTBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, TurretBaseNTBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (blockEntity() instanceof TurretFritzBlockEntity
                && stack.getItem() instanceof FluidIdentifierItem) {
            var moved =
                    moveItemStackTo(
                            stack, FRITZ_SERVICE_MENU_SLOT, FRITZ_SERVICE_MENU_SLOT + 1, false);
            if (moved) {
                return true;
            }
        }
        if (stack.is(ModItems.TURRET_CHIP.get())) {
            return moveItemStackTo(
                    stack,
                    TurretBaseNTBlockEntity.SLOT_CHIP,
                    TurretBaseNTBlockEntity.SLOT_CHIP + 1,
                    false);
        }
        return moveItemStackTo(
                stack,
                TurretBaseNTBlockEntity.SLOT_AMMO_FIRST,
                TurretBaseNTBlockEntity.SLOT_COUNT,
                false);
    }
}
