/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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
import com.hbm.blockentity.machine.TemplateCrafterBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotPattern;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AutocrafterMenu extends BlockEntityMenu<TemplateCrafterBlockEntity> {

    public AutocrafterMenu(
            int containerId, Inventory playerInventory, TemplateCrafterBlockEntity blockEntity) {
        super(ModMenus.MACHINE_AUTOCRAFTER.get(), containerId, blockEntity);

        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 3; column++) {
                int index = TemplateCrafterBlockEntity.TEMPLATE_START + row * 3 + column;
                int x = 44 + column * 18;
                int y = 22 + row * 18;
                addSlot(new SlotPattern(blockEntity, index, x, y));
            }
        }
        addSlot(
                new SlotPattern(blockEntity, TemplateCrafterBlockEntity.PREVIEW_SLOT, 116, 40)
                        .allowStackSize());

        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 3; column++) {
                int index = TemplateCrafterBlockEntity.INGREDIENT_START + row * 3 + column;
                int x = 44 + column * 18;
                int y = 86 + row * 18;
                addSlot(SlotFiltered.gated(blockEntity, index, x, y));
            }
        }
        addSlot(new Slot(blockEntity, TemplateCrafterBlockEntity.OUTPUT_SLOT, 116, 104));
        addSlot(new Slot(blockEntity, TemplateCrafterBlockEntity.BATTERY_SLOT, 17, 99));
        addStandardInventorySlots(playerInventory, 8, 158);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < TemplateCrafterBlockEntity.TEMPLATE_START
                || slotIndex > TemplateCrafterBlockEntity.PREVIEW_SLOT) {
            super.clicked(slotIndex, button, input, player);
            return;
        }

        var slot = getSlot(slotIndex);
        boolean secondaryPickup = input == ContainerInput.PICKUP && button == 1;
        boolean occupied = !slot.getItem().isEmpty();
        if (slotIndex == TemplateCrafterBlockEntity.PREVIEW_SLOT) {
            if (secondaryPickup && occupied) {
                blockEntity().cycleRecipe();
                broadcastChanges();
            }
            return;
        }

        if (secondaryPickup && occupied) {
            blockEntity().advanceFilterMode(slotIndex);
            return;
        }
        slot.set(getCarried());
        var machine = blockEntity();
        machine.patternMatcher().initPatternSmart(player.level(), slot.getItem(), slotIndex);
        machine.rebuildTemplate();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex >= TemplateCrafterBlockEntity.TEMPLATE_START
                && slotIndex < TemplateCrafterBlockEntity.INGREDIENT_START) {
            return ItemStack.EMPTY;
        }
        return quickMove(
                player,
                slotIndex,
                false,
                stack -> {
                    if (slotIndex < TemplateCrafterBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, TemplateCrafterBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    if (stack.getItem() instanceof EnergyItem) {
                        return moveItemStackTo(
                                stack,
                                TemplateCrafterBlockEntity.BATTERY_SLOT,
                                TemplateCrafterBlockEntity.BATTERY_SLOT + 1,
                                false);
                    }
                    return false;
                });
    }
}
