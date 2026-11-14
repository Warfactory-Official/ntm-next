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
import com.hbm.blockentity.machine.HydrotreaterBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class HydrotreaterMenu extends BlockEntityMenu<HydrotreaterBlockEntity> {

    private final SyncedData data;

    public HydrotreaterMenu(int containerId, Inventory playerInv, HydrotreaterBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(HydrotreaterBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private HydrotreaterMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            HydrotreaterBlockEntity be) {
        super(ModMenus.MACHINE_HYDROTREATER.get(), containerId, be, container);
        checkContainerSize(container, HydrotreaterBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, HydrotreaterBlockEntity.SLOT_BATTERY, 17, 90));
        addSlot(new Slot(container, HydrotreaterBlockEntity.SLOT_INPUT_IN, 35, 90));
        addSlot(
                new SlotFiltered(
                        container,
                        HydrotreaterBlockEntity.SLOT_INPUT_OUT,
                        35,
                        108,
                        SlotFiltered.NONE));
        addSlot(new Slot(container, HydrotreaterBlockEntity.SLOT_OUTPUT_IN_START, 125, 90));
        addSlot(
                new SlotFiltered(
                        container,
                        HydrotreaterBlockEntity.SLOT_OUTPUT_OUT_START,
                        125,
                        108,
                        SlotFiltered.NONE));
        addSlot(new Slot(container, HydrotreaterBlockEntity.SLOT_OUTPUT_IN_START + 2, 143, 90));
        addSlot(
                new SlotFiltered(
                        container,
                        HydrotreaterBlockEntity.SLOT_OUTPUT_OUT_START + 2,
                        143,
                        108,
                        SlotFiltered.NONE));
        addSlot(new Slot(container, HydrotreaterBlockEntity.SLOT_FLUID_ID, 17, 108));
        addSlot(new Slot(container, HydrotreaterBlockEntity.SLOT_CATALYST, 89, 36));

        addStandardInventorySlots(playerInv, 8, 156);
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
                false,
                stack -> {
                    int machineEnd = HydrotreaterBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                HydrotreaterBlockEntity.SLOT_FLUID_ID,
                                HydrotreaterBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                HydrotreaterBlockEntity.SLOT_BATTERY,
                                HydrotreaterBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (stack.is(ModItems.CATALYTIC_CONVERTER.get()))
                        return moveItemStackTo(
                                stack,
                                HydrotreaterBlockEntity.SLOT_CATALYST,
                                HydrotreaterBlockEntity.SLOT_CATALYST + 1,
                                false);
                    return moveItemStackTo(
                                    stack,
                                    HydrotreaterBlockEntity.SLOT_INPUT_IN,
                                    HydrotreaterBlockEntity.SLOT_INPUT_IN + 1,
                                    false)
                            || moveItemStackTo(
                                    stack,
                                    HydrotreaterBlockEntity.SLOT_OUTPUT_IN_START,
                                    HydrotreaterBlockEntity.SLOT_OUTPUT_IN_START + 1,
                                    false)
                            || moveItemStackTo(
                                    stack,
                                    HydrotreaterBlockEntity.SLOT_OUTPUT_IN_START + 2,
                                    HydrotreaterBlockEntity.SLOT_OUTPUT_IN_START + 3,
                                    false);
                });
    }
}
