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

import com.hbm.blockentity.bomb.NukeManBlockEntity;
import com.hbm.inventory.slot.SlotComponent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NukeManMenu extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = NukeManBlockEntity.SLOT_COUNT;

    public NukeManMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public NukeManMenu(int containerId, Inventory playerInv, NukeManBlockEntity nuke) {
        this(containerId, playerInv, (Container) nuke);
    }

    private NukeManMenu(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.NUKE_MAN.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, NukeManBlockEntity.SLOT_IGNITER, 26, 35));
        addSlot(new SlotComponent(container, NukeManBlockEntity.SLOT_LENS_1, 8, 17));
        addSlot(new SlotComponent(container, NukeManBlockEntity.SLOT_LENS_2, 44, 17));
        addSlot(new SlotComponent(container, NukeManBlockEntity.SLOT_LENS_3, 8, 53));
        addSlot(new SlotComponent(container, NukeManBlockEntity.SLOT_LENS_4, 44, 53));
        addSlot(new SlotComponent(container, NukeManBlockEntity.SLOT_CORE, 98, 35));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return NukeManBlockEntity.isReady(container());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack ->
                        index < MACHINE_SLOTS
                                && moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true));
    }
}
