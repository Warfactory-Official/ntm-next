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
import com.hbm.blockentity.processing.ShredderBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.BladesItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ShredderMenu extends NtmContainerMenu {

    private final SyncedData data;

    public ShredderMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(ShredderBlockEntity.SLOT_COUNT),
                SyncedData.client(ShredderBlockEntity.class));
    }

    public ShredderMenu(
            int containerId, Inventory playerInventory, ShredderBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, SyncedData.of(blockEntity));
    }

    private ShredderMenu(
            int containerId, Inventory playerInventory, Container inventory, SyncedData data) {
        super(ModMenus.MACHINE_SHREDDER.get(), containerId, inventory);
        checkContainerSize(inventory, ShredderBlockEntity.SLOT_COUNT);
        this.data = data;

        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 3; column++) {
                int index = ShredderBlockEntity.SLOT_INPUT_START + row * 3 + column;
                int x = 44 + column * 18;
                int y = 18 + row * 18;
                addSlot(new Slot(inventory, index, x, y));
            }
        }
        for (var row = 0; row < 6; row++) {
            for (var column = 0; column < 3; column++) {
                int index = ShredderBlockEntity.SLOT_OUTPUT_START + row * 3 + column;
                int x = 116 + column * 18;
                int y = 18 + row * 18;
                addSlot(new SlotRecipeOutput(playerInventory.player, inventory, index, x, y));
            }
        }
        addSlot(new Slot(inventory, ShredderBlockEntity.SLOT_BLADE_LEFT, 44, 108));
        addSlot(new Slot(inventory, ShredderBlockEntity.SLOT_BLADE_RIGHT, 80, 108));
        addSlot(new Slot(inventory, ShredderBlockEntity.SLOT_BATTERY, 8, 108));
        addStandardInventorySlots(playerInventory, 8, 151);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public int getProgress() {
        return data.getInt("progress");
    }

    public int getProgressScaled(int size) {
        return getProgress() * size / ShredderBlockEntity.PROCESSING_SPEED;
    }

    public long getPowerScaled(long size) {
        return getPower() * size / ShredderBlockEntity.MAX_POWER;
    }

    public int getGearLeft() {
        return ShredderBlockEntity.gearOf(container().getItem(ShredderBlockEntity.SLOT_BLADE_LEFT));
    }

    public int getGearRight() {
        return ShredderBlockEntity.gearOf(
                container().getItem(ShredderBlockEntity.SLOT_BLADE_RIGHT));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                true,
                stack -> {
                    if (slotIndex < ShredderBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, ShredderBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (EnergyItem.matches(stack)) {
            return moveItemStackTo(
                    stack,
                    ShredderBlockEntity.SLOT_BATTERY,
                    ShredderBlockEntity.SLOT_BATTERY + 1,
                    false);
        }
        if (stack.getItem() instanceof BladesItem) {
            return moveItemStackTo(
                    stack,
                    ShredderBlockEntity.SLOT_BLADE_LEFT,
                    ShredderBlockEntity.SLOT_BLADE_RIGHT + 1,
                    false);
        }
        return moveItemStackTo(
                stack,
                ShredderBlockEntity.SLOT_INPUT_START,
                ShredderBlockEntity.SLOT_INPUT_END,
                false);
    }
}
