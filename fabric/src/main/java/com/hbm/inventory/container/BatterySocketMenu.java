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
import com.hbm.blockentity.machine.storage.SocketBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BatterySocketMenu extends BlockEntityMenu<SocketBlockEntity> {

    private final SyncedData data;

    public BatterySocketMenu(int containerId, Inventory playerInv, SocketBlockEntity socket) {
        this(
                containerId,
                playerInv,
                socket,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(SocketBlockEntity.class)
                        : SyncedData.of(socket),
                socket);
    }

    private BatterySocketMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            SocketBlockEntity socket) {
        super(ModMenus.BATTERY_SOCKET.get(), containerId, socket, container);
        checkContainerSize(container, SocketBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container, SocketBlockEntity.SLOT_BATTERY, 35, 35, EnergyItem::matches));
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

    public long getDelta() {
        return data.get("delta");
    }

    public ItemStack getBattery() {
        return container().getItem(SocketBlockEntity.SLOT_BATTERY);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = SocketBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                SocketBlockEntity.SLOT_BATTERY,
                                SocketBlockEntity.SLOT_BATTERY + 1,
                                false);
                    return false;
                });
    }
}
