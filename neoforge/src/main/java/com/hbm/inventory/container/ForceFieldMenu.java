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

import com.hbm.blockentity.machine.RepulsionFieldBlockEntity;
import com.hbm.data.MachineData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ForceFieldMenu extends BlockEntityMenu<RepulsionFieldBlockEntity> {

    private final SyncedData data;

    public ForceFieldMenu(int containerId, Inventory playerInv, RepulsionFieldBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(RepulsionFieldBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private ForceFieldMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            RepulsionFieldBlockEntity be) {
        super(ModMenus.FORCEFIELD.get(), containerId, be, container);
        checkContainerSize(container, RepulsionFieldBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, RepulsionFieldBlockEntity.SLOT_BATTERY, 26, 53));
        addSlot(new Slot(container, RepulsionFieldBlockEntity.SLOT_RADIUS, 89, 35));
        addSlot(new Slot(container, RepulsionFieldBlockEntity.SLOT_HEALTH, 107, 35));

        addStandardInventorySlots(playerInv, 8, 84);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("storedEnergy");
    }

    public int getPowerScaled(int i) {
        return (int) (getPower() * i / MachineData.FORCE_FIELD_MAX_POWER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = RepulsionFieldBlockEntity.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (moveItemStackTo(
                            stack,
                            RepulsionFieldBlockEntity.SLOT_RADIUS,
                            RepulsionFieldBlockEntity.SLOT_HEALTH + 1,
                            false)) {
                        return true;
                    }
                    return moveItemStackTo(
                            stack,
                            RepulsionFieldBlockEntity.SLOT_BATTERY,
                            RepulsionFieldBlockEntity.SLOT_BATTERY + 1,
                            false);
                });
    }
}
