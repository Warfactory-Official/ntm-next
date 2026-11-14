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

import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.items.machine.RBMKRodItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RBMKRodMenu extends BlockEntityMenu<RBMKRodBlockEntity> {

    public RBMKRodMenu(int containerId, Inventory playerInv, RBMKRodBlockEntity rod) {
        this(containerId, playerInv, rod, rod);
    }

    private RBMKRodMenu(
            int containerId, Inventory playerInv, Container container, RBMKRodBlockEntity rod) {
        super(ModMenus.RBMK_ROD.get(), containerId, rod, container);
        checkContainerSize(container, 1);

        addSlot(
                new Slot(container, 0, 80, 45) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof RBMKRodItem;
                    }
                });

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public void clicked(
            int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex == 0
                && !player.getAbilities().instabuild
                && !blockEntity().coldEnoughForManual()) return;
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = 1;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, machineEnd, false);
                });
    }
}
