/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.container;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.blockentity.machine.oil.PyroOvenBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PyroOvenMenu extends BlockEntityMenu<PyroOvenBlockEntity> {

    public PyroOvenMenu(int containerId, Inventory playerInv, PyroOvenBlockEntity be) {
        super(ModMenus.MACHINE_PYROOVEN.get(), containerId, be);
        checkContainerSize(be, PyroOvenBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be, PyroOvenBlockEntity.SLOT_BATTERY, 152, 72, EnergyItem::matches));
        addSlot(new Slot(be, PyroOvenBlockEntity.SLOT_ITEM_INPUT, 35, 45));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, PyroOvenBlockEntity.SLOT_ITEM_OUTPUT, 89, 45));
        addSlot(
                new SlotFiltered(
                        be,
                        PyroOvenBlockEntity.SLOT_FLUID_ID,
                        8,
                        72,
                        s -> s.getItem() instanceof FluidIdentifierItem));
        addSlot(new SlotUpgrade(be, PyroOvenBlockEntity.SLOT_UPGRADE_START, 71, 72));
        addSlot(new SlotUpgrade(be, PyroOvenBlockEntity.SLOT_UPGRADE_END, 89, 72));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = PyroOvenBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                PyroOvenBlockEntity.SLOT_FLUID_ID,
                                PyroOvenBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                PyroOvenBlockEntity.SLOT_BATTERY,
                                PyroOvenBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                PyroOvenBlockEntity.SLOT_UPGRADE_START,
                                PyroOvenBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            PyroOvenBlockEntity.SLOT_ITEM_INPUT,
                            PyroOvenBlockEntity.SLOT_ITEM_INPUT + 1,
                            false);
                });
    }
}
