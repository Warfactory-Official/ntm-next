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

import com.hbm.blockentity.machine.thermal.CoolingHeatBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HeatexMenu extends BlockEntityMenu<CoolingHeatBlockEntity> {

    public HeatexMenu(int containerId, Inventory playerInv, CoolingHeatBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private HeatexMenu(
            int containerId, Inventory playerInv, Container container, CoolingHeatBlockEntity be) {
        super(ModMenus.HEATER_HEATEX.get(), containerId, be, container);
        checkContainerSize(container, 1);
        container.startOpen(playerInv.player);

        addSlot(
                new SlotFiltered(
                        container,
                        0,
                        80,
                        72,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index == 0) return moveItemStackTo(stack, 1, slots.size(), true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(stack, 0, 1, false);
                    return false;
                });
    }
}
