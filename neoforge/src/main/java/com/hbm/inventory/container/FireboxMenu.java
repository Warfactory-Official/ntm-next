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

import com.hbm.blockentity.machine.HeaterFireboxBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FireboxMenu extends BlockEntityMenu<HeaterFireboxBlockEntity> {

    private static final int SLOT_COUNT = 2;
    private final boolean opensContainer;

    public FireboxMenu(int containerId, Inventory playerInv, HeaterFireboxBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private FireboxMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            HeaterFireboxBlockEntity be) {
        super(ModMenus.HEATER_FIREBOX.get(), containerId, be, container);
        checkContainerSize(container, SLOT_COUNT);
        opensContainer = !playerInv.player.isSpectator();
        if (opensContainer) container.startOpen(playerInv.player);

        addSlot(new Slot(container, 0, 44, 27));
        addSlot(new Slot(container, 1, 62, 27));

        addStandardInventorySlots(playerInv, 8, 86);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (opensContainer) container().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, SLOT_COUNT);
    }
}
