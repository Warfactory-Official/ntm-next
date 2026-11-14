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

import com.hbm.inventory.slot.SlotPattern;
import com.hbm.platform.Platform;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RebarPlacerMenu extends NtmContainerMenu {
    public RebarPlacerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(1));
    }

    public RebarPlacerMenu(int containerId, Inventory playerInventory, Container itemInventory) {
        super(ModMenus.REBAR_PLACER.get(), containerId, itemInventory);
        addSlot(new SlotPattern(itemInventory, 0, 53, 36));
        addStandardInventorySlots(playerInventory, 8, 100);
    }

    public Slot pattern() {
        return getSlot(0);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput action, Player player) {
        if (slotIndex == 0) {
            var carried = getCarried();
            if (Platform.canFitInsideContainerItems(carried)) {
                pattern().set(carried);
            }
            return;
        }
        super.clicked(slotIndex, button, action, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
