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
import com.hbm.blockentity.machine.oil.LiquefactorBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LiquefactorMenu extends BlockEntityMenu<LiquefactorBlockEntity> {

    public LiquefactorMenu(int containerId, Inventory playerInv, LiquefactorBlockEntity be) {
        super(ModMenus.MACHINE_LIQUEFACTOR.get(), containerId, be);
        checkContainerSize(be, LiquefactorBlockEntity.SLOT_COUNT);

        addSlot(new Slot(be, LiquefactorBlockEntity.SLOT_INPUT, 35, 54));
        addSlot(
                new SlotFiltered(
                        be, LiquefactorBlockEntity.SLOT_BATTERY, 134, 72, EnergyItem::matches));
        addSlot(new SlotUpgrade(be, LiquefactorBlockEntity.SLOT_UPGRADE_START, 98, 36));
        addSlot(new SlotUpgrade(be, LiquefactorBlockEntity.SLOT_UPGRADE_END, 98, 54));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = LiquefactorBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                LiquefactorBlockEntity.SLOT_BATTERY,
                                LiquefactorBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                LiquefactorBlockEntity.SLOT_UPGRADE_START,
                                LiquefactorBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            LiquefactorBlockEntity.SLOT_INPUT,
                            LiquefactorBlockEntity.SLOT_INPUT + 1,
                            false);
                });
    }
}
