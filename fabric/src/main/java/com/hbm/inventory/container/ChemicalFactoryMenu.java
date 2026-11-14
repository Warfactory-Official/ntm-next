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
import com.hbm.blockentity.processing.ChemicalFactoryBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ChemicalFactoryMenu extends BlockEntityMenu<ChemicalFactoryBlockEntity> {

    public ChemicalFactoryMenu(
            int containerId, Inventory playerInv, ChemicalFactoryBlockEntity be) {
        super(ModMenus.MACHINE_CHEMICAL_FACTORY.get(), containerId, be);
        checkContainerSize(be, ChemicalFactoryBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        ChemicalFactoryBlockEntity.SLOT_BATTERY,
                        224,
                        88,
                        EnergyItem::matches));
        for (int k = 0; k < 3; k++) {
            addSlot(
                    new SlotUpgrade(
                            container(),
                            ChemicalFactoryBlockEntity.SLOT_UPGRADE_START + k,
                            206,
                            125 + k * 18));
        }

        for (int i = 0; i < ChemicalFactoryBlockEntity.MODULES; i++) {
            int base = i * ChemicalFactoryBlockEntity.SLOTS_PER_MODULE;
            int y = 20 + i * 22;
            addSlot(
                    new SlotFiltered(
                            container(),
                            4 + base,
                            93,
                            y,
                            stack -> stack.getItem() instanceof BlueprintsItem));
            for (int j = 0; j < 3; j++)
                addSlot(SlotFiltered.gated(container(), 5 + base + j, 10 + j * 16, y));
            for (int j = 0; j < ChemicalFactoryBlockEntity.OUTPUTS_PER_MODULE; j++) {
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player,
                                container(),
                                ChemicalFactoryBlockEntity.OUTPUT_OFFSET + base + j,
                                139 + j * 16,
                                y));
            }
        }

        addStandardInventorySlots(playerInv, 26, 134);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = ChemicalFactoryBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                ChemicalFactoryBlockEntity.SLOT_BATTERY,
                                ChemicalFactoryBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                ChemicalFactoryBlockEntity.SLOT_UPGRADE_START,
                                ChemicalFactoryBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    if (stack.getItem() instanceof BlueprintsItem) return moveToAnyModule(stack, 4);
                    return moveToAnyModuleInputs(stack);
                });
    }

    private boolean moveToAnyModule(ItemStack stack, int moduleOffset) {
        for (int i = 0; i < ChemicalFactoryBlockEntity.MODULES; i++) {
            int slot = i * ChemicalFactoryBlockEntity.SLOTS_PER_MODULE + moduleOffset;
            if (moveItemStackTo(stack, slot, slot + 1, false)) return true;
        }
        return false;
    }

    private boolean moveToAnyModuleInputs(ItemStack stack) {
        for (int i = 0; i < ChemicalFactoryBlockEntity.MODULES; i++) {
            int base = i * ChemicalFactoryBlockEntity.SLOTS_PER_MODULE;
            if (moveItemStackTo(stack, 5 + base, 8 + base, false)) return true;
        }
        return false;
    }
}
