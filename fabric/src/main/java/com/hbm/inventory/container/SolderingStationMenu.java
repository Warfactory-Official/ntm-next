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
import com.hbm.blockentity.processing.SolderingBenchBlockEntity;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SolderingStationMenu extends BlockEntityMenu<SolderingBenchBlockEntity> {

    private final SyncedData data;

    public SolderingStationMenu(
            int containerId, Inventory playerInv, SolderingBenchBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(SolderingBenchBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private SolderingStationMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            SolderingBenchBlockEntity be) {
        super(ModMenus.MACHINE_SOLDERING_STATION.get(), containerId, be, container);
        checkContainerSize(container, SolderingBenchBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(
                SlotFiltered.gated(
                        container, SolderingBenchBlockEntity.SLOT_TOPPING_START, 17, 18));
        addSlot(
                SlotFiltered.gated(
                        container, SolderingBenchBlockEntity.SLOT_TOPPING_START + 1, 35, 18));
        addSlot(
                SlotFiltered.gated(
                        container, SolderingBenchBlockEntity.SLOT_TOPPING_START + 2, 53, 18));
        addSlot(SlotFiltered.gated(container, SolderingBenchBlockEntity.SLOT_PCB_START, 17, 36));
        addSlot(
                SlotFiltered.gated(
                        container, SolderingBenchBlockEntity.SLOT_PCB_START + 1, 35, 36));
        addSlot(SlotFiltered.gated(container, SolderingBenchBlockEntity.SLOT_SOLDER, 53, 36));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        SolderingBenchBlockEntity.SLOT_OUTPUT,
                        107,
                        27));
        addSlot(
                new SlotFiltered(
                        container,
                        SolderingBenchBlockEntity.SLOT_BATTERY,
                        152,
                        72,
                        EnergyItem::matches));
        addSlot(new Slot(container, SolderingBenchBlockEntity.SLOT_FLUID_ID, 17, 63));
        addSlot(new SlotUpgrade(container, SolderingBenchBlockEntity.SLOT_UPGRADE_START, 89, 63));
        addSlot(new SlotUpgrade(container, SolderingBenchBlockEntity.SLOT_UPGRADE_END, 107, 63));

        addStandardInventorySlots(playerInv, 8, 122);
        addDataSlots(data);
    }

    private static boolean matchesAny(List<CountIngredient> group, ItemStack stack) {
        for (CountIngredient ing : group) if (ing.matchesItem(stack)) return true;
        return false;
    }

    public int getProgress() {
        return data.getInt("progress");
    }

    public int getProcessTime() {
        return data.getInt("processTime");
    }

    public int getProgressScaled(int i) {
        int max = getProcessTime();
        return max <= 0 ? 0 : Math.min(i, getProgress() * i / max);
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

    public boolean isCollisionPrevention() {
        return data.getBoolean("collisionPrevention");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = SolderingBenchBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                SolderingBenchBlockEntity.SLOT_FLUID_ID,
                                SolderingBenchBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                SolderingBenchBlockEntity.SLOT_BATTERY,
                                SolderingBenchBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                SolderingBenchBlockEntity.SLOT_UPGRADE_START,
                                SolderingBenchBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    if (matchesAny(SolderingRecipes.toppings(), stack))
                        return moveItemStackTo(
                                stack,
                                SolderingBenchBlockEntity.SLOT_TOPPING_START,
                                SolderingBenchBlockEntity.SLOT_TOPPING_START + 3,
                                false);
                    if (matchesAny(SolderingRecipes.pcb(), stack))
                        return moveItemStackTo(
                                stack,
                                SolderingBenchBlockEntity.SLOT_PCB_START,
                                SolderingBenchBlockEntity.SLOT_PCB_START + 2,
                                false);
                    if (matchesAny(SolderingRecipes.solder(), stack))
                        return moveItemStackTo(
                                stack,
                                SolderingBenchBlockEntity.SLOT_SOLDER,
                                SolderingBenchBlockEntity.SLOT_SOLDER + 1,
                                false);
                    return false;
                });
    }
}
