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
import com.hbm.blockentity.processing.ChemicalPlantBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.ModItems;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ChemicalPlantMenu extends BlockEntityMenu<ChemicalPlantBlockEntity> {

    private final SyncedData data;

    public ChemicalPlantMenu(int containerId, Inventory playerInv, ChemicalPlantBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(ChemicalPlantBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private ChemicalPlantMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            ChemicalPlantBlockEntity be) {
        super(ModMenus.MACHINE_CHEMICAL_PLANT.get(), containerId, be, container);
        checkContainerSize(container, ChemicalPlantBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        ChemicalPlantBlockEntity.SLOT_BATTERY,
                        152,
                        81,
                        stack ->
                                EnergyItem.matches(stack)
                                        || stack.is(ModItems.METEORITE_SWORD_MACHINED.get())));
        addSlot(
                new SlotFiltered(
                        container,
                        ChemicalPlantBlockEntity.SLOT_SCHEMATIC,
                        35,
                        126,
                        stack -> stack.getItem() instanceof BlueprintsItem));
        addSlot(new SlotUpgrade(container, ChemicalPlantBlockEntity.SLOT_UPGRADE_START, 152, 108));
        addSlot(
                new SlotUpgrade(
                        container, ChemicalPlantBlockEntity.SLOT_UPGRADE_START + 1, 152, 126));

        for (int i = 0; i < ChemicalPlantBlockEntity.TANK_COUNT; i++) {
            addSlot(
                    SlotFiltered.gated(
                            container,
                            ChemicalPlantBlockEntity.SLOT_ITEM_IN_START + i,
                            8 + i * 18,
                            99));
        }
        for (int i = 0; i < ChemicalPlantBlockEntity.TANK_COUNT; i++) {
            addSlot(
                    new SlotRecipeOutput(
                            playerInv.player,
                            container,
                            ChemicalPlantBlockEntity.SLOT_ITEM_OUT_START + i,
                            80 + i * 18,
                            99));
        }

        for (int i = 0; i < ChemicalPlantBlockEntity.TANK_COUNT; i++) {
            addSlot(
                    new Slot(
                            container,
                            ChemicalPlantBlockEntity.SLOT_FLUID_IN_START + i,
                            8 + i * 18,
                            54));
        }
        for (int i = 0; i < ChemicalPlantBlockEntity.TANK_COUNT; i++) {
            addSlot(
                    new SlotFiltered(
                            container,
                            ChemicalPlantBlockEntity.SLOT_FLUID_IN_RESULT + i,
                            8 + i * 18,
                            72,
                            SlotFiltered.NONE));
        }
        for (int i = 0; i < ChemicalPlantBlockEntity.TANK_COUNT; i++) {
            addSlot(
                    new Slot(
                            container,
                            ChemicalPlantBlockEntity.SLOT_FLUID_OUT_START + i,
                            80 + i * 18,
                            54));
        }
        for (int i = 0; i < ChemicalPlantBlockEntity.TANK_COUNT; i++) {
            addSlot(
                    new SlotFiltered(
                            container,
                            ChemicalPlantBlockEntity.SLOT_FLUID_OUT_RESULT + i,
                            80 + i * 18,
                            72,
                            SlotFiltered.NONE));
        }

        addStandardInventorySlots(playerInv, 8, 174);
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
                    int machineEnd = ChemicalPlantBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                ChemicalPlantBlockEntity.SLOT_BATTERY,
                                ChemicalPlantBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof BlueprintsItem)
                        return moveItemStackTo(
                                stack,
                                ChemicalPlantBlockEntity.SLOT_SCHEMATIC,
                                ChemicalPlantBlockEntity.SLOT_SCHEMATIC + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                ChemicalPlantBlockEntity.SLOT_UPGRADE_START,
                                ChemicalPlantBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            ChemicalPlantBlockEntity.SLOT_ITEM_IN_START,
                            ChemicalPlantBlockEntity.SLOT_ITEM_OUT_START,
                            false);
                });
    }
}
