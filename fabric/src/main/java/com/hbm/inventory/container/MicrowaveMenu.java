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

import com.hbm.blockentity.machine.MicrowaveBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MicrowaveMenu extends BlockEntityMenu<MicrowaveBlockEntity> {

    public MicrowaveMenu(int containerId, Inventory playerInv, MicrowaveBlockEntity be) {
        super(ModMenus.MACHINE_MICROWAVE.get(), containerId, be);
        checkContainerSize(be, MicrowaveBlockEntity.SLOT_COUNT);

        addSlot(new Slot(be, MicrowaveBlockEntity.INPUT_SLOT, 80, 35));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, MicrowaveBlockEntity.OUTPUT_SLOT, 140, 35));
        addSlot(new Slot(be, MicrowaveBlockEntity.BATTERY_SLOT, 8, 53));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = MicrowaveBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(
                                    stack,
                                    MicrowaveBlockEntity.INPUT_SLOT,
                                    MicrowaveBlockEntity.INPUT_SLOT + 1,
                                    false)
                            || moveItemStackTo(
                                    stack,
                                    MicrowaveBlockEntity.BATTERY_SLOT,
                                    MicrowaveBlockEntity.BATTERY_SLOT + 1,
                                    false);
                });
    }
}
