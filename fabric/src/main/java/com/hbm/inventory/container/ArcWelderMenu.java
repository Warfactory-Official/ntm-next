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
import com.hbm.blockentity.processing.ArcWelderBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArcWelderMenu extends BlockEntityMenu<ArcWelderBlockEntity> {

    private final SyncedData data;

    public ArcWelderMenu(int containerId, Inventory playerInventory, ArcWelderBlockEntity machine) {
        super(ModMenus.MACHINE_ARC_WELDER.get(), containerId, machine);

        for (var column = 0; column < 3; column++) {
            var x = 17 + column * 18;
            addSlot(new Slot(machine, column, x, 36));
        }
        addSlot(
                new SlotRecipeOutput(
                        playerInventory.player,
                        machine,
                        ArcWelderBlockEntity.SLOT_OUTPUT,
                        107,
                        36));
        addSlot(new Slot(machine, ArcWelderBlockEntity.SLOT_BATTERY, 152, 72));
        addSlot(new Slot(machine, ArcWelderBlockEntity.SLOT_FLUID_ID, 17, 63));
        for (var column = 0; column < 2; column++) {
            var x = 89 + column * 18;
            addSlot(
                    new SlotUpgrade(
                            machine, ArcWelderBlockEntity.SLOT_UPGRADE_START + column, x, 63));
        }

        addStandardInventorySlots(playerInventory, 8, 122);

        data =
                playerInventory.player.level().isClientSide()
                        ? SyncedData.client(ArcWelderBlockEntity.class)
                        : SyncedData.of(machine);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                true,
                stack -> {
                    if (slotIndex < ArcWelderBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, ArcWelderBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    if (stack.getItem() instanceof FluidIdentifierItem) {
                        return moveItemStackTo(
                                stack,
                                ArcWelderBlockEntity.SLOT_FLUID_ID,
                                ArcWelderBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    }
                    if (EnergyItem.matches(stack)) {
                        return moveItemStackTo(
                                stack,
                                ArcWelderBlockEntity.SLOT_BATTERY,
                                ArcWelderBlockEntity.SLOT_BATTERY + 1,
                                false);
                    }
                    if (MachineUpgradeItem.isUpgrade(stack)) {
                        return moveItemStackTo(
                                stack,
                                ArcWelderBlockEntity.SLOT_UPGRADE_START,
                                ArcWelderBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    }
                    return moveItemStackTo(stack, 0, 3, false);
                });
    }

    public int getProgress() {
        return data.getInt("progress");
    }

    public int getProcessTime() {
        return data.getInt("processTime");
    }

    public int getProgressScaled(int scale) {
        var processTime = getProcessTime();
        if (processTime <= 0) {
            return 0;
        }
        return Math.min(scale, getProgress() * scale / processTime);
    }

    public long getPower() {
        return data.get("power");
    }

    public long getMaxPower() {
        return data.get("maxPower");
    }

    public long getConsumption() {
        return data.get("consumption");
    }
}
