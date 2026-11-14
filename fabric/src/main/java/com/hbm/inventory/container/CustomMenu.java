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
import com.hbm.blockentity.machine.CustomMachineBlockEntity;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotPattern;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CustomMenu extends BlockEntityMenu<CustomMachineBlockEntity> {
    private static final int[] COLUMN_X = {8, 26, 44};
    private static final int[] ROW_Y = {72, 90};

    private final int fluidIdentifierEnd;
    private final int machineSlotCount;

    public CustomMenu(int containerId, Inventory playerInventory, CustomMachineBlockEntity be) {
        super(ModMenus.MACHINE_CUSTOM.get(), containerId, be);
        checkContainerSize(be, CustomMachineBlockEntity.INVENTORY_SIZE);
        CustomMachineDefinition definition = be.definition;
        int fluidIn =
                definition == null
                        ? 0
                        : Math.min(
                                definition.fluidInCount(), CustomMachineBlockEntity.FLUID_COLUMNS);
        int itemIn =
                definition == null
                        ? 0
                        : Math.min(definition.itemInCount(), CustomMachineBlockEntity.ITEM_COLUMNS);
        int itemOut =
                definition == null
                        ? 0
                        : Math.min(
                                definition.itemOutCount(), CustomMachineBlockEntity.ITEM_COLUMNS);

        addSlot(
                new SlotFiltered(
                        be, CustomMachineBlockEntity.BATTERY_SLOT, 150, 72, EnergyItem::matches));

        for (int i = 0; i < fluidIn; i++) {
            int slot = CustomMachineBlockEntity.FLUID_ID_START + i;
            addSlot(
                    new SlotFiltered(
                            be,
                            slot,
                            8 + 18 * i,
                            54,
                            stack -> stack.getItem() instanceof FluidIdentifierItem));
        }
        fluidIdentifierEnd = slots.size();
        for (int i = 0; i < itemIn; i++) {
            int slot = CustomMachineBlockEntity.INPUT_START + i;
            addSlot(
                    new SlotFiltered(
                            be,
                            slot,
                            COLUMN_X[i % 3],
                            ROW_Y[i / 3],
                            stack -> be.canPlaceItem(slot, stack)));
        }
        for (int i = 0; i < itemIn; i++) {
            addSlot(
                    new SlotPattern(
                            be,
                            CustomMachineBlockEntity.TEMPLATE_START + i,
                            COLUMN_X[i % 3],
                            108 + 18 * (i / 3)));
        }
        for (int i = 0; i < itemOut; i++) {
            addSlot(
                    new SlotRecipeOutput(
                            playerInventory.player,
                            be,
                            CustomMachineBlockEntity.OUTPUT_START + i,
                            78 + 18 * (i % 3),
                            ROW_Y[i / 3]));
        }

        machineSlotCount = slots.size();
        addStandardInventorySlots(playerInventory, 8, 174);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0
                || slotIndex >= slots.size()
                || !(getSlot(slotIndex) instanceof SlotPattern)) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        CustomMachineBlockEntity be = blockEntity();
        Slot slot = getSlot(slotIndex);
        int index = slot.getContainerSlot() - CustomMachineBlockEntity.TEMPLATE_START;

        if (button == 1 && input == ContainerInput.PICKUP && slot.hasItem()) {
            be.patterns.nextMode(be.getLevel(), slot.getItem(), index);
            be.markChanged();
            return;
        }
        slot.set(getCarried());
        be.patterns.initPatternSmart(be.getLevel(), slot.getItem(), index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < machineSlotCount) return false;
                    if (stack.getItem() instanceof FluidIdentifierItem) {
                        for (var slot = 1; slot < fluidIdentifierEnd; slot++) {
                            if (moveItemStackTo(stack, slot, slot + 1, false)) return true;
                        }
                    }
                    return false;
                });
    }
}
