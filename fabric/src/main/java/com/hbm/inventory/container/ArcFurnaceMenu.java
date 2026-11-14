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
import com.hbm.blockentity.processing.ArcFurnaceBlockEntity;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ArcElectrodeItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArcFurnaceMenu extends BlockEntityMenu<ArcFurnaceBlockEntity> {

    public ArcFurnaceMenu(
            int containerId, Inventory playerInventory, ArcFurnaceBlockEntity machine) {
        super(ModMenus.MACHINE_ARC_FURNACE.get(), containerId, machine);

        for (var column = 0; column < 3; column++) {
            var x = 62 + column * 18;
            addSlot(
                    new SlotFiltered(
                            machine,
                            column,
                            x,
                            22,
                            stack -> stack.getItem() instanceof ArcElectrodeItem));
        }
        addSlot(new Slot(machine, 3, 8, 108));
        addSlot(new Slot(machine, 4, 152, 108));

        for (var row = 0; row < 4; row++) {
            for (var column = 0; column < 5; column++) {
                var backingSlot = ArcFurnaceBlockEntity.SLOT_GRID_START + row * 5 + column;
                var x = 44 + column * 18;
                var y = 54 + row * 18;
                addSlot(
                        new InPlaceSmeltingSlot(
                                playerInventory.player, machine, backingSlot, x, y));
            }
        }

        var queueStart = ArcFurnaceBlockEntity.SLOT_GRID_END + 1;
        for (var backingSlot = queueStart;
                backingSlot < ArcFurnaceBlockEntity.SLOT_COUNT;
                backingSlot++) {
            var x = 44 + (backingSlot - queueStart) * 18;
            addSlot(SlotFiltered.gated(machine, backingSlot, x, 129));
        }

        addStandardInventorySlots(playerInventory, 8, 174);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                true,
                stack -> {
                    if (slotIndex < ArcFurnaceBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, ArcFurnaceBlockEntity.SLOT_COUNT, slots.size(), true);
                    }

                    var item = stack.getItem();
                    if (item instanceof EnergyItem || item == ModItems.BATTERY_CREATIVE.get()) {
                        return moveItemStackTo(stack, 3, 4, false);
                    }
                    if (item instanceof ArcElectrodeItem) {
                        return moveItemStackTo(stack, 0, 3, false);
                    }
                    if (MachineUpgradeItem.isUpgrade(stack)) {
                        return moveItemStackTo(stack, 4, 5, false);
                    }
                    return moveItemStackTo(
                            stack,
                            ArcFurnaceBlockEntity.SLOT_GRID_END + 1,
                            ArcFurnaceBlockEntity.SLOT_COUNT,
                            false);
                });
    }

    private static final class InPlaceSmeltingSlot extends SlotRecipeOutput {

        private final ArcFurnaceBlockEntity machine;

        private InPlaceSmeltingSlot(
                Player player, ArcFurnaceBlockEntity machine, int slotIndex, int x, int y) {
            super(player, machine, slotIndex, x, y);
            this.machine = machine;
        }

        @Override
        public boolean mayPlace(ItemStack candidate) {
            if (machine.liquidMode) {
                return true;
            }

            var recipe = ArcFurnaceRecipes.INSTANCE.getOutput(candidate, false);
            if (recipe == null) {
                return false;
            }
            var output = recipe.solidOutput();
            if (output == null) {
                return false;
            }

            var producedCount = (long) output.getCount() * candidate.getCount();
            return producedCount <= output.getMaxStackSize()
                    && candidate.getCount() <= machine.getMaxInputSize();
        }

        @Override
        public int getMaxStackSize() {
            return getItem().isEmpty() ? 1 : machine.getMaxInputSize();
        }
    }
}
