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

import com.hbm.entity.train.TrainCargoTramTrailer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TrainCargoTramTrailerMenu extends NtmContainerMenu {

    public TrainCargoTramTrailerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(TrainCargoTramTrailer.SLOT_COUNT));
    }

    public TrainCargoTramTrailerMenu(
            int containerId, Inventory playerInventory, TrainCargoTramTrailer trailer) {
        this(containerId, playerInventory, (Container) trailer);
    }

    private TrainCargoTramTrailerMenu(int containerId, Inventory playerInventory, Container cargo) {
        super(ModMenus.TRAIN_CARGO_TRAM_TRAILER.get(), containerId, cargo);

        for (var backingSlot = 0; backingSlot < TrainCargoTramTrailer.SLOT_COUNT; backingSlot++) {
            var column = backingSlot % 9;
            var row = backingSlot / 9;
            var x = 8 + column * 18;
            var y = 18 + row * 18;
            addSlot(new Slot(cargo, backingSlot, x, y));
        }
        addStandardInventorySlots(playerInventory, 8, 140);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMoveUnsorted(player, slotIndex, TrainCargoTramTrailer.SLOT_COUNT);
    }
}
