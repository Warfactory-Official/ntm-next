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

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.blockentity.machine.oil.RefineryBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RefineryMenu extends BlockEntityMenu<RefineryBlockEntity> {

    private final SyncedData data;

    public RefineryMenu(
            int containerId, Inventory playerInventory, RefineryBlockEntity blockEntity) {
        super(ModMenus.MACHINE_REFINERY.get(), containerId, blockEntity);
        data =
                playerInventory.player.level().isClientSide()
                        ? SyncedData.client(RefineryBlockEntity.class)
                        : SyncedData.of(blockEntity);

        addSlot(new Slot(blockEntity, RefineryBlockEntity.SLOT_BATTERY, 158, 108));
        addContainerPair(blockEntity, 1, 12);
        for (var column = 0; column < 4; column++) {
            int index = 3 + column * 2;
            int x = 64 + column * 18;
            addContainerPair(blockEntity, index, x);
        }
        addSlot(
                new SlotRecipeOutput(
                        playerInventory.player,
                        blockEntity,
                        RefineryBlockEntity.SLOT_BYPRODUCT,
                        38,
                        90));
        addSlot(new Slot(blockEntity, RefineryBlockEntity.SLOT_FLUID_ID, 38, 108));
        addStandardInventorySlots(playerInventory, 11, 158);
        addDataSlots(data);
    }

    private void addContainerPair(RefineryBlockEntity blockEntity, int inputIndex, int x) {
        addSlot(new Slot(blockEntity, inputIndex, x, 90));
        addSlot(new SlotFiltered(blockEntity, inputIndex + 1, x, 108, SlotFiltered.NONE));
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                true,
                stack -> {
                    if (slotIndex < RefineryBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, RefineryBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (stack.getItem() instanceof FluidIdentifierItem
                && moveItemStackTo(
                        stack,
                        RefineryBlockEntity.SLOT_FLUID_ID,
                        RefineryBlockEntity.SLOT_FLUID_ID + 1,
                        false)) {
            return true;
        }
        if (EnergyItem.matches(stack)) {
            return moveItemStackTo(
                    stack,
                    RefineryBlockEntity.SLOT_BATTERY,
                    RefineryBlockEntity.SLOT_BATTERY + 1,
                    false);
        }
        for (var inputIndex = 1; inputIndex < RefineryBlockEntity.SLOT_BYPRODUCT; inputIndex += 2) {
            if (moveItemStackTo(stack, inputIndex, inputIndex + 1, false)) {
                return true;
            }
        }
        return moveItemStackTo(
                stack,
                RefineryBlockEntity.SLOT_FLUID_ID,
                RefineryBlockEntity.SLOT_FLUID_ID + 1,
                false);
    }
}
