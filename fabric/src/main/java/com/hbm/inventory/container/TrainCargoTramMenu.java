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
import com.hbm.entity.train.TrainCargoTram;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TrainCargoTramMenu extends NtmContainerMenu {

    private final ContainerData powerData;

    public TrainCargoTramMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(TrainCargoTram.SLOT_COUNT),
                new SimpleContainerData(1));
    }

    public TrainCargoTramMenu(int containerId, Inventory playerInventory, TrainCargoTram tram) {
        this(
                containerId,
                playerInventory,
                tram,
                new ContainerData() {
                    @Override
                    public int get(int dataIndex) {
                        return tram.getPower();
                    }

                    @Override
                    public void set(int dataIndex, int value) {
                        tram.setPower(value);
                    }

                    @Override
                    public int getCount() {
                        return 1;
                    }
                });
    }

    private TrainCargoTramMenu(
            int containerId, Inventory playerInventory, Container cargo, ContainerData powerData) {
        super(ModMenus.TRAIN_CARGO_TRAM.get(), containerId, cargo);
        this.powerData = powerData;

        for (var backingSlot = 0; backingSlot < TrainCargoTram.SLOT_CHARGE; backingSlot++) {
            var column = backingSlot % 7;
            var row = backingSlot / 7;
            var x = 8 + column * 18;
            var y = 18 + row * 18;
            addSlot(new Slot(cargo, backingSlot, x, y));
        }
        addSlot(new Slot(cargo, TrainCargoTram.SLOT_CHARGE, 152, 72));
        addStandardInventorySlots(playerInventory, 8, 122);
        addDataSlots(powerData);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                false,
                stack -> {
                    if (slotIndex < TrainCargoTram.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, TrainCargoTram.SLOT_COUNT, slots.size(), true);
                    }
                    if (EnergyItem.matches(stack)) {
                        return moveItemStackTo(
                                stack,
                                TrainCargoTram.SLOT_CHARGE,
                                TrainCargoTram.SLOT_COUNT,
                                false);
                    }
                    return moveItemStackTo(stack, 0, TrainCargoTram.SLOT_CHARGE, false);
                });
    }

    public int getPower() {
        return powerData.get(0);
    }

    public int getMaxPower() {
        return 1_000;
    }

    public int getPowerConsumption() {
        return 10;
    }
}
