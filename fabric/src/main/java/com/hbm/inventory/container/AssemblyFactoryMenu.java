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
import com.hbm.blockentity.machine.AssemblyFactoryBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AssemblyFactoryMenu extends BlockEntityMenu<AssemblyFactoryBlockEntity> {

    private static final int MODULES = AssemblyFactoryBlockEntity.MODULES;
    private static final int SLOTS_PER_MODULE = AssemblyFactoryBlockEntity.SLOTS_PER_MODULE;
    private static final int TEMPLATE_OFFSET = AssemblyFactoryBlockEntity.TEMPLATE_OFFSET;
    private static final int INPUT_OFFSET = AssemblyFactoryBlockEntity.INPUT_OFFSET;
    private static final int OUTPUT_OFFSET = AssemblyFactoryBlockEntity.OUTPUT_OFFSET;

    public AssemblyFactoryMenu(
            int containerId, Inventory playerInv, AssemblyFactoryBlockEntity be) {
        super(ModMenus.MACHINE_ASSEMBLY_FACTORY.get(), containerId, be);
        checkContainerSize(be, AssemblyFactoryBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(),
                        AssemblyFactoryBlockEntity.SLOT_BATTERY,
                        234,
                        112,
                        EnergyItem::matches));
        for (int k = 0; k < 3; k++) {
            addSlot(
                    new SlotUpgrade(
                            container(),
                            AssemblyFactoryBlockEntity.SLOT_UPGRADE_START + k,
                            214,
                            149 + k * 18));
        }

        for (int i = 0; i < MODULES; i++) {
            int base = i * SLOTS_PER_MODULE;
            int colX = (i % 2) * 109;
            int rowY = (i / 2) * 56;

            addSlot(
                    new SlotFiltered(
                            container(),
                            base + TEMPLATE_OFFSET,
                            25 + colX,
                            54 + rowY,
                            stack -> stack.getItem() instanceof BlueprintsItem));

            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 6; col++) {
                    int slot = base + INPUT_OFFSET + col + row * 6;
                    addSlot(
                            SlotFiltered.gated(
                                    container(), slot, 7 + colX + col * 16, 20 + rowY + row * 16));
                }
            }

            addSlot(
                    new SlotRecipeOutput(
                            playerInv.player,
                            container(),
                            base + OUTPUT_OFFSET,
                            87 + colX,
                            54 + rowY));
        }

        addStandardInventorySlots(playerInv, 33, 158);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = AssemblyFactoryBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                AssemblyFactoryBlockEntity.SLOT_BATTERY,
                                AssemblyFactoryBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                AssemblyFactoryBlockEntity.SLOT_UPGRADE_START,
                                AssemblyFactoryBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    if (stack.getItem() instanceof BlueprintsItem)
                        return moveToAnyModule(stack, TEMPLATE_OFFSET);
                    return moveToAnyModuleInputs(stack);
                });
    }

    private boolean moveToAnyModule(ItemStack stack, int moduleOffset) {
        for (int i = 0; i < MODULES; i++) {
            int slot = i * SLOTS_PER_MODULE + moduleOffset;
            if (moveItemStackTo(stack, slot, slot + 1, false)) return true;
        }
        return false;
    }

    private boolean moveToAnyModuleInputs(ItemStack stack) {
        for (int i = 0; i < MODULES; i++) {
            int base = i * SLOTS_PER_MODULE;
            if (moveItemStackTo(stack, base + INPUT_OFFSET, base + OUTPUT_OFFSET, false))
                return true;
        }
        return false;
    }
}
