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

import com.hbm.blockentity.machine.thermal.IronFurnaceBlockEntity;
import com.hbm.inventory.slot.SlotSmelting;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FurnaceIronMenu extends BlockEntityMenu<IronFurnaceBlockEntity> {

    public FurnaceIronMenu(int containerId, Inventory playerInv, IronFurnaceBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private FurnaceIronMenu(
            int containerId, Inventory playerInv, Container container, IronFurnaceBlockEntity be) {
        super(ModMenus.FURNACE_IRON.get(), containerId, be, container);
        checkContainerSize(container, IronFurnaceBlockEntity.SLOT_COUNT);

        addSlot(new Slot(container, IronFurnaceBlockEntity.SLOT_INPUT, 53, 17));
        addSlot(new Slot(container, IronFurnaceBlockEntity.SLOT_FUEL_A, 53, 53));
        addSlot(new Slot(container, IronFurnaceBlockEntity.SLOT_FUEL_B, 71, 53));
        addSlot(
                new SlotSmelting(
                        playerInv.player, be, IronFurnaceBlockEntity.SLOT_OUTPUT, 125, 35));
        addSlot(new SlotUpgrade(container, IronFurnaceBlockEntity.SLOT_UPGRADE, 17, 35));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = IronFurnaceBlockEntity.SLOT_COUNT;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                IronFurnaceBlockEntity.SLOT_UPGRADE,
                                IronFurnaceBlockEntity.SLOT_UPGRADE + 1,
                                false);
                    if (container().canPlaceItem(IronFurnaceBlockEntity.SLOT_FUEL_A, stack))
                        return moveItemStackTo(
                                stack,
                                IronFurnaceBlockEntity.SLOT_FUEL_A,
                                IronFurnaceBlockEntity.SLOT_FUEL_B + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            IronFurnaceBlockEntity.SLOT_INPUT,
                            IronFurnaceBlockEntity.SLOT_INPUT + 1,
                            false);
                });
    }
}
