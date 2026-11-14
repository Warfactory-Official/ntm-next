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

import com.hbm.entity.cart.MinecartDestroyerEntity;
import com.hbm.inventory.slot.SlotPattern;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class CartDestroyerMenu extends NtmContainerMenu {
    private static final int BANK_SLOT_COUNT = MinecartDestroyerEntity.FILTER_SLOTS / 2;

    public CartDestroyerMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(MinecartDestroyerEntity.FILTER_SLOTS));
    }

    public CartDestroyerMenu(int containerId, Inventory playerInventory, Container filters) {
        super(ModMenus.CART_DESTROYER.get(), containerId, filters);

        for (var index = 0; index < BANK_SLOT_COUNT; index++) {
            var column = index % 3;
            var row = index / 3;
            var y = 17 + row * 18;
            var firstX = 10 + column * 18;
            var secondX = 114 + column * 18;
            addSlot(new SlotPattern(filters, index, firstX, y));
            addSlot(new SlotPattern(filters, index + BANK_SLOT_COUNT, secondX, y));
        }
        addStandardInventorySlots(playerInventory, 8, 84);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput action, Player player) {
        if (slotIndex >= 0 && slotIndex < MinecartDestroyerEntity.FILTER_SLOTS) {
            getSlot(slotIndex).set(getCarried());
            return;
        }
        super.clicked(slotIndex, button, action, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
