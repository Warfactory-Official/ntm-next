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

import com.hbm.blockentity.network.CraneExtractorBlockEntity;
import com.hbm.inventory.slot.SlotPattern;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CraneExtractorMenu extends BlockEntityMenu<CraneExtractorBlockEntity> {
    public CraneExtractorMenu(
            int containerId, Inventory playerInventory, CraneExtractorBlockEntity machine) {
        super(ModMenus.CRANE_EXTRACTOR.get(), containerId, machine);
        checkContainerSize(machine, CraneExtractorBlockEntity.SLOT_COUNT);

        for (var index = 0; index < CraneExtractorBlockEntity.FILTER_SLOTS; index++) {
            var x = 71 + index % 3 * 18;
            var y = 17 + index / 3 * 18;
            addSlot(new SlotPattern(machine, index, x, y));
        }
        for (var index = CraneExtractorBlockEntity.SLOT_BUFFER;
                index < CraneExtractorBlockEntity.SLOT_UPGRADE_STACK;
                index++) {
            var bufferIndex = index - CraneExtractorBlockEntity.SLOT_BUFFER;
            var x = 8 + bufferIndex % 3 * 18;
            var y = 17 + bufferIndex / 3 * 18;
            addSlot(new Slot(machine, index, x, y));
        }
        addSlot(new SlotUpgrade(machine, CraneExtractorBlockEntity.SLOT_UPGRADE_STACK, 152, 23));
        addSlot(new SlotUpgrade(machine, CraneExtractorBlockEntity.SLOT_UPGRADE_EJECTOR, 152, 47));
        addStandardInventorySlots(playerInventory, 26, 103);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput action, Player player) {
        if (player.isSpectator()) {
            return;
        }
        if (slotIndex >= 0 && slotIndex < CraneExtractorBlockEntity.FILTER_SLOTS) {
            if (action == ContainerInput.QUICK_MOVE) {
                return;
            }

            var machine = blockEntity();
            var template = getSlot(slotIndex);
            if (action == ContainerInput.PICKUP && button == 1 && !template.getItem().isEmpty()) {
                machine.advanceFilterMode(slotIndex);
            } else {
                template.set(getCarried());
                machine.matcher.initPatternStandard(
                        machine.getLevel(), template.getItem(), slotIndex);
            }
            return;
        }
        super.clicked(slotIndex, button, action, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.isSpectator() || index < CraneExtractorBlockEntity.FILTER_SLOTS) {
            return ItemStack.EMPTY;
        }
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < CraneExtractorBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, CraneExtractorBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (MachineUpgradeItem.getLevel(stack, UpgradeType.STACK) > 0) {
            return moveItemStackTo(
                    stack,
                    CraneExtractorBlockEntity.SLOT_UPGRADE_STACK,
                    CraneExtractorBlockEntity.SLOT_UPGRADE_STACK + 1,
                    false);
        }
        if (MachineUpgradeItem.getLevel(stack, UpgradeType.EJECTOR) > 0) {
            return moveItemStackTo(
                    stack,
                    CraneExtractorBlockEntity.SLOT_UPGRADE_EJECTOR,
                    CraneExtractorBlockEntity.SLOT_UPGRADE_EJECTOR + 1,
                    false);
        }
        return moveItemStackTo(
                stack,
                CraneExtractorBlockEntity.SLOT_BUFFER,
                CraneExtractorBlockEntity.SLOT_COUNT,
                false);
    }
}
