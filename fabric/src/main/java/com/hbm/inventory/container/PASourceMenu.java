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
import com.hbm.blockentity.machine.albion.PASourceBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PASourceMenu extends BlockEntityMenu<PASourceBlockEntity> {

    public PASourceMenu(int containerId, Inventory playerInv, PASourceBlockEntity be) {
        super(ModMenus.PA_SOURCE.get(), containerId, be);
        checkContainerSize(be, PASourceBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        container(), PASourceBlockEntity.SLOT_BATTERY, 8, 72, EnergyItem::matches));
        addSlot(new Slot(container(), PASourceBlockEntity.SLOT_INPUT_1, 62, 16));
        addSlot(new Slot(container(), PASourceBlockEntity.SLOT_INPUT_2, 80, 16));
        addSlot(
                new SlotFiltered(
                        container(),
                        PASourceBlockEntity.SLOT_CONTAINER_1,
                        62,
                        43,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container(),
                        PASourceBlockEntity.SLOT_CONTAINER_2,
                        80,
                        43,
                        SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = PASourceBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                PASourceBlockEntity.SLOT_BATTERY,
                                PASourceBlockEntity.SLOT_BATTERY + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            PASourceBlockEntity.SLOT_INPUT_1,
                            PASourceBlockEntity.SLOT_INPUT_2 + 1,
                            false);
                });
    }
}
