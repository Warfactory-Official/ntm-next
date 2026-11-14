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
import com.hbm.blockentity.machine.ExcavatorBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.DrillbitItem;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ExcavatorMenu extends BlockEntityMenu<ExcavatorBlockEntity> {

    private final SyncedData data;

    public ExcavatorMenu(int containerId, Inventory playerInv, ExcavatorBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(ExcavatorBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private ExcavatorMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            ExcavatorBlockEntity be) {
        super(ModMenus.MACHINE_EXCAVATOR.get(), containerId, be, container);
        checkContainerSize(container, ExcavatorBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, ExcavatorBlockEntity.SLOT_BATTERY, 220, 72));
        addSlot(new Slot(container, ExcavatorBlockEntity.SLOT_FLUID_ID, 202, 72));
        for (int i = 0; i < 3; i++) {
            addSlot(
                    new Slot(
                            container,
                            ExcavatorBlockEntity.SLOT_UPGRADE_START + i,
                            136 + i * 18,
                            75));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(
                        new SlotFiltered(
                                container,
                                ExcavatorBlockEntity.SLOT_BUFFER_START + col + row * 3,
                                136 + col * 18,
                                5 + row * 18,
                                SlotFiltered.NONE));
            }
        }

        addStandardInventorySlots(playerInv, 41, 122);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public int getPowerScaled(int i) {
        return (int) (getPower() * i / ExcavatorBlockEntity.MAX_POWER);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = ExcavatorBlockEntity.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                ExcavatorBlockEntity.SLOT_FLUID_ID,
                                ExcavatorBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                ExcavatorBlockEntity.SLOT_BATTERY,
                                ExcavatorBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                ExcavatorBlockEntity.SLOT_UPGRADE_START,
                                ExcavatorBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    if (stack.getItem() instanceof DrillbitItem)
                        return moveItemStackTo(
                                stack,
                                ExcavatorBlockEntity.SLOT_DRILL,
                                ExcavatorBlockEntity.SLOT_DRILL + 1,
                                false);
                    return false;
                });
    }
}
