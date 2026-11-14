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
import com.hbm.blockentity.processing.CrystalProcessorBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CrystallizerMenu extends BlockEntityMenu<CrystalProcessorBlockEntity> {

    private final SyncedData data;

    public CrystallizerMenu(int containerId, Inventory playerInv, CrystalProcessorBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(CrystalProcessorBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private CrystallizerMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            CrystalProcessorBlockEntity be) {
        super(ModMenus.MACHINE_CRYSTALLIZER.get(), containerId, be, container);
        checkContainerSize(container, CrystalProcessorBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, CrystalProcessorBlockEntity.SLOT_INPUT, 62, 45));
        addSlot(
                new SlotFiltered(
                        container,
                        CrystalProcessorBlockEntity.SLOT_BATTERY,
                        152,
                        72,
                        EnergyItem::matches));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        CrystalProcessorBlockEntity.SLOT_OUTPUT,
                        113,
                        45));
        addSlot(new Slot(container, CrystalProcessorBlockEntity.SLOT_FLUID_IN, 17, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        CrystalProcessorBlockEntity.SLOT_FLUID_OUT,
                        17,
                        54,
                        SlotFiltered.NONE));
        addSlot(new SlotUpgrade(container, CrystalProcessorBlockEntity.SLOT_UPGRADE_START, 80, 18));
        addSlot(new SlotUpgrade(container, CrystalProcessorBlockEntity.SLOT_UPGRADE_END, 98, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        CrystalProcessorBlockEntity.SLOT_FLUID_ID,
                        35,
                        72,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 122);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = CrystalProcessorBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                CrystalProcessorBlockEntity.SLOT_FLUID_ID,
                                CrystalProcessorBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                CrystalProcessorBlockEntity.SLOT_BATTERY,
                                CrystalProcessorBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                CrystalProcessorBlockEntity.SLOT_UPGRADE_START,
                                CrystalProcessorBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            CrystalProcessorBlockEntity.SLOT_INPUT,
                            CrystalProcessorBlockEntity.SLOT_INPUT + 1,
                            false);
                });
    }
}
