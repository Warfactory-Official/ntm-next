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
import com.hbm.blockentity.machine.storage.BatteryBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BatteryMenu extends BlockEntityMenu<BatteryBlockEntity> {

    private final SyncedData data;

    public BatteryMenu(int containerId, Inventory playerInv, BatteryBlockEntity battery) {
        this(
                containerId,
                playerInv,
                battery,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(BatteryBlockEntity.class)
                        : SyncedData.of(battery),
                battery);
    }

    private BatteryMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            BatteryBlockEntity battery) {
        super(ModMenus.MACHINE_BATTERY.get(), containerId, battery, container);
        checkContainerSize(container, BatteryBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container, BatteryBlockEntity.SLOT_IN, 26, 17, EnergyItem::matches));
        addSlot(
                new SlotFiltered(
                        container, BatteryBlockEntity.SLOT_OUT, 26, 53, EnergyItem::matches));
        addStandardInventorySlots(playerInv, 8, 84);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public long getMaxPower() {
        return blockEntity().getMaxPower();
    }

    public long getPowerRemainingScaled(long i) {
        return blockEntity().fillScaled(i);
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = BatteryBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack, BatteryBlockEntity.SLOT_IN, machineEnd, false);
                    return false;
                });
    }
}
