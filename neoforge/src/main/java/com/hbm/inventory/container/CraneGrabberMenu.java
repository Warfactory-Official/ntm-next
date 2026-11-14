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

import com.hbm.blockentity.network.CraneGrabberBlockEntity;
import com.hbm.inventory.slot.SlotPattern;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CraneGrabberMenu extends BlockEntityMenu<CraneGrabberBlockEntity> {

    public CraneGrabberMenu(
            int containerId, Inventory playerInventory, CraneGrabberBlockEntity be) {
        super(ModMenus.CRANE_GRABBER.get(), containerId, be);
        checkContainerSize(be, CraneGrabberBlockEntity.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new SlotPattern(be, j + i * 3, 40 + j * 18, 17 + i * 18));
            }
        }

        addSlot(new SlotUpgrade(be, CraneGrabberBlockEntity.SLOT_UPGRADE_STACK, 121, 23));
        addSlot(new SlotUpgrade(be, CraneGrabberBlockEntity.SLOT_UPGRADE_EJECTOR, 121, 47));

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (player.isSpectator()) return;
        if (slotIndex < 0 || slotIndex >= CraneGrabberBlockEntity.FILTER_SLOTS) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        CraneGrabberBlockEntity grabber = blockEntity();
        if (input == ContainerInput.QUICK_MOVE) return;
        Slot slot = getSlot(slotIndex);

        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            grabber.advanceFilterMode(slotIndex);
            return;
        }

        slot.set(getCarried());
        grabber.matcher.initPatternStandard(grabber.getLevel(), slot.getItem(), slotIndex);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.isSpectator()) return ItemStack.EMPTY;
        if (index < CraneGrabberBlockEntity.FILTER_SLOTS) return ItemStack.EMPTY;

        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < CraneGrabberBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, CraneGrabberBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    if (MachineUpgradeItem.getLevel(stack, UpgradeType.STACK) > 0) {
                        return moveItemStackTo(
                                stack,
                                CraneGrabberBlockEntity.SLOT_UPGRADE_STACK,
                                CraneGrabberBlockEntity.SLOT_UPGRADE_STACK + 1,
                                false);
                    }
                    if (MachineUpgradeItem.getLevel(stack, UpgradeType.EJECTOR) > 0) {
                        return moveItemStackTo(
                                stack,
                                CraneGrabberBlockEntity.SLOT_UPGRADE_EJECTOR,
                                CraneGrabberBlockEntity.SLOT_UPGRADE_EJECTOR + 1,
                                false);
                    }
                    return false;
                });
    }
}
