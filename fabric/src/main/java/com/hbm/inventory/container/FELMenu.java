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

import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blockentity.machine.WavelengthLaserBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FELMenu extends BlockEntityMenu<WavelengthLaserBlockEntity> {

    public FELMenu(int containerId, Inventory playerInv, WavelengthLaserBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private FELMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            WavelengthLaserBlockEntity be) {
        super(ModMenus.MACHINE_FEL.get(), containerId, be, container);
        checkContainerSize(container, WavelengthLaserBlockEntity.SLOT_COUNT);

        addSlot(new Slot(container, WavelengthLaserBlockEntity.SLOT_BATTERY, 182, 144));
        addSlot(new Slot(container, WavelengthLaserBlockEntity.SLOT_CRYSTAL, 141, 23));

        addStandardInventorySlots(playerInv, 8, 83);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = WavelengthLaserBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, invEnd, false);
                    var source = slots.get(index);
                    int destination =
                            ItemEnergyTransfer.isBattery(
                                            source.container, source.getContainerSlot())
                                    ? WavelengthLaserBlockEntity.SLOT_BATTERY
                                    : WavelengthLaserBlockEntity.SLOT_CRYSTAL;
                    return moveItemStackTo(stack, destination, destination + 1, false);
                });
    }
}
