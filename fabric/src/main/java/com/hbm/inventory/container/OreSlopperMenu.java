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

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.blockentity.machine.OreSlopperBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OreSlopperMenu extends BlockEntityMenu<OreSlopperBlockEntity> {

    public OreSlopperMenu(
            int containerId, Inventory playerInventory, OreSlopperBlockEntity blockEntity) {
        super(ModMenus.MACHINE_ORE_SLOPPER.get(), containerId, blockEntity);

        addSlot(new Slot(blockEntity, OreSlopperBlockEntity.SLOT_BATTERY, 8, 72));
        addSlot(new Slot(blockEntity, OreSlopperBlockEntity.SLOT_FLUID_ID, 26, 72));
        addSlot(new Slot(blockEntity, OreSlopperBlockEntity.SLOT_INPUT, 71, 27));
        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 2; column++) {
                int index = OreSlopperBlockEntity.SLOT_OUTPUT_START + row * 2 + column;
                int x = 134 + column * 18;
                int y = 18 + row * 18;
                addSlot(new SlotRecipeOutput(playerInventory.player, blockEntity, index, x, y));
            }
        }
        for (var column = 0; column < 2; column++) {
            int index = OreSlopperBlockEntity.SLOT_UPGRADE_START + column;
            int x = 62 + column * 18;
            addSlot(new Slot(blockEntity, index, x, 72));
        }
        addStandardInventorySlots(playerInventory, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                true,
                stack -> {
                    if (slotIndex < OreSlopperBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, OreSlopperBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (stack.getItem() instanceof FluidIdentifierItem) {
            return moveItemStackTo(
                    stack,
                    OreSlopperBlockEntity.SLOT_FLUID_ID,
                    OreSlopperBlockEntity.SLOT_FLUID_ID + 1,
                    false);
        }
        if (EnergyItem.matches(stack)) {
            return moveItemStackTo(
                    stack,
                    OreSlopperBlockEntity.SLOT_BATTERY,
                    OreSlopperBlockEntity.SLOT_BATTERY + 1,
                    false);
        }
        if (MachineUpgradeItem.isUpgrade(stack)) {
            return moveItemStackTo(
                    stack,
                    OreSlopperBlockEntity.SLOT_UPGRADE_START,
                    OreSlopperBlockEntity.SLOT_UPGRADE_END + 1,
                    false);
        }
        if (stack.is(ModItems.BEDROCK_ORE_BASE.get())) {
            return moveItemStackTo(
                    stack,
                    OreSlopperBlockEntity.SLOT_INPUT,
                    OreSlopperBlockEntity.SLOT_INPUT + 1,
                    false);
        }
        return false;
    }
}
