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

import com.hbm.blockentity.machine.AshpitBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AshpitMenu extends BlockEntityMenu<AshpitBlockEntity> {

    private final boolean opensContainer;

    public AshpitMenu(int containerId, Inventory playerInv, AshpitBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private AshpitMenu(
            int containerId, Inventory playerInv, Container container, AshpitBlockEntity be) {
        super(ModMenus.MACHINE_ASHPIT.get(), containerId, be, container);
        checkContainerSize(container, 5);
        opensContainer = !playerInv.player.isSpectator();
        if (opensContainer) container.startOpen(playerInv.player);

        for (int i = 0; i < 5; i++) {
            addSlot(new SlotFiltered(container, i, 44 + i * 18, 27, SlotFiltered.NONE));
        }

        addStandardInventorySlots(playerInv, 8, 86);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (opensContainer) container().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index > 4) return false;
                    return moveItemStackTo(stack, 5, slots.size(), true);
                });
    }
}
