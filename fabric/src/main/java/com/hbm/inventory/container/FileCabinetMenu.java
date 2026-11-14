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

import com.hbm.blockentity.machine.storage.FileCabinetBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FileCabinetMenu extends BlockEntityMenu<FileCabinetBlockEntity> {

    private final boolean openedSession;

    public FileCabinetMenu(
            int containerId, Inventory playerInventory, FileCabinetBlockEntity machine) {
        super(ModMenus.FILE_CABINET.get(), containerId, machine);

        var storage = container();
        openedSession = !playerInventory.player.isSpectator();
        if (openedSession) {
            storage.startOpen(playerInventory.player);
        }

        for (var backingSlot = 0; backingSlot < FileCabinetBlockEntity.SLOT_COUNT; backingSlot++) {
            var column = backingSlot % 4;
            var row = backingSlot / 4;
            var x = 53 + column * 18;
            var y = 18 + row * 36;
            addSlot(new Slot(storage, backingSlot, x, y));
        }
        addStandardInventorySlots(playerInventory, 8, 88);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMoveUnsorted(player, slotIndex, FileCabinetBlockEntity.SLOT_COUNT);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (openedSession) {
            container().stopOpen(player);
        }
    }
}
