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

import com.hbm.api.item.TargetDesignator;
import com.hbm.blockentity.ordnance.launch.LaunchSiloBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LaunchPadRustedMenu extends BlockEntityMenu<LaunchSiloBlockEntity> {
    private static final int SLOTS = LaunchSiloBlockEntity.SLOT_COUNT;

    public LaunchPadRustedMenu(int id, Inventory playerInventory, LaunchSiloBlockEntity pad) {
        this(id, playerInventory, pad, pad);
    }

    private LaunchPadRustedMenu(
            int id, Inventory playerInventory, Container container, LaunchSiloBlockEntity pad) {
        super(ModMenus.LAUNCH_PAD_RUSTED.get(), id, pad, container);
        checkContainerSize(container, SLOTS);
        addSlot(new SlotRecipeOutput(playerInventory.player, container, 0, 26, 72));
        addSlot(new Slot(container, LaunchSiloBlockEntity.CODE_SLOT, 116, 45));
        addSlot(new Slot(container, LaunchSiloBlockEntity.KEY_SLOT, 134, 45));
        addSlot(new Slot(container, LaunchSiloBlockEntity.DESIGNATOR_SLOT, 26, 99));
        addStandardInventorySlots(playerInventory, 8, 154);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(blockEntity(), player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index < SLOTS) return moveItemStackTo(stack, SLOTS, slots.size(), true);
                    if (stack.getItem() instanceof TargetDesignator)
                        return moveItemStackTo(stack, 3, 4, false);
                    if (stack.is(ModItems.LAUNCH_CODE.get()))
                        return moveItemStackTo(stack, 1, 2, false);
                    if (stack.is(ModItems.LAUNCH_KEY.get()))
                        return moveItemStackTo(stack, 2, 3, false);
                    return false;
                });
    }
}
