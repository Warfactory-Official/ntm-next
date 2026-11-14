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

import com.hbm.blockentity.machine.storage.BatteryBlockEntity;
import com.hbm.blockentity.machine.storage.ReddBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BatteryREDDMenu extends BlockEntityMenu<ReddBlockEntity> {

    private final SyncedData data;

    public BatteryREDDMenu(int containerId, Inventory playerInv, ReddBlockEntity battery) {
        this(
                containerId,
                playerInv,
                battery,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(ReddBlockEntity.class)
                        : SyncedData.of(battery),
                battery);
    }

    private BatteryREDDMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            ReddBlockEntity battery) {
        super(ModMenus.BATTERY_REDD.get(), containerId, battery, container);
        checkContainerSize(container, BatteryBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(SlotFiltered.gated(container, BatteryBlockEntity.SLOT_IN, 26, 53));
        addSlot(SlotFiltered.gated(container, BatteryBlockEntity.SLOT_OUT, 80, 53));
        addStandardInventorySlots(playerInv, 8, 99);
        addDataSlots(data);
    }

    public int getRedLow() {
        return data.getInt("redLow");
    }

    public int getRedHigh() {
        return data.getInt("redHigh");
    }

    public int getPriorityOrdinal() {
        return data.getInt("priority");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BatteryBlockEntity.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    return moveItemStackTo(stack, 0, machineEnd, false);
                });
    }
}
