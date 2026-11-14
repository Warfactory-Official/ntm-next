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
import com.hbm.blockentity.processing.AssemblyMachineBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.ModItems;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AssemblyMachineMenu extends BlockEntityMenu<AssemblyMachineBlockEntity> {

    private final SyncedData data;

    public AssemblyMachineMenu(
            int containerId, Inventory playerInv, AssemblyMachineBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(AssemblyMachineBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private AssemblyMachineMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            AssemblyMachineBlockEntity be) {
        super(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), containerId, be, container);
        checkContainerSize(container, AssemblyMachineBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        AssemblyMachineBlockEntity.SLOT_BATTERY,
                        152,
                        81,
                        stack ->
                                EnergyItem.matches(stack)
                                        || stack.is(ModItems.METEORITE_SWORD_ALLOYED.get())));
        addSlot(
                new SlotFiltered(
                        container,
                        AssemblyMachineBlockEntity.SLOT_BLUEPRINT,
                        35,
                        126,
                        stack -> stack.getItem() instanceof BlueprintsItem));
        addSlot(
                new SlotUpgrade(
                        container, AssemblyMachineBlockEntity.SLOT_UPGRADE_START, 152, 108));
        addSlot(
                new SlotUpgrade(
                        container, AssemblyMachineBlockEntity.SLOT_UPGRADE_START + 1, 152, 126));

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = AssemblyMachineBlockEntity.SLOT_INPUT_START + row * 3 + col;
                addSlot(SlotFiltered.gated(container, slot, 8 + col * 18, 18 + row * 18));
            }
        }

        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        AssemblyMachineBlockEntity.SLOT_OUTPUT,
                        98,
                        45));

        addStandardInventorySlots(playerInv, 8, 174);
        addDataSlots(data);
    }

    public int getProgressScaled(int i) {
        return (int) Math.ceil(i * blockEntity().recipeModule.progress);
    }

    public long getPower() {
        return data.get("power");
    }

    public long getMaxPower() {
        return data.get("maxPower");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = AssemblyMachineBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                AssemblyMachineBlockEntity.SLOT_BATTERY,
                                AssemblyMachineBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof BlueprintsItem)
                        return moveItemStackTo(
                                stack,
                                AssemblyMachineBlockEntity.SLOT_BLUEPRINT,
                                AssemblyMachineBlockEntity.SLOT_BLUEPRINT + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                AssemblyMachineBlockEntity.SLOT_UPGRADE_START,
                                AssemblyMachineBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            AssemblyMachineBlockEntity.SLOT_INPUT_START,
                            AssemblyMachineBlockEntity.SLOT_INPUT_END + 1,
                            false);
                });
    }
}
