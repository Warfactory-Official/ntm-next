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

import com.hbm.blockentity.machine.rbmk.RBMKHeaterBlockEntity;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RBMKHeaterMenu extends BlockEntityMenu<RBMKHeaterBlockEntity> {

    public RBMKHeaterMenu(int containerId, Inventory playerInv, RBMKHeaterBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private RBMKHeaterMenu(
            int containerId, Inventory playerInv, Container container, RBMKHeaterBlockEntity be) {
        super(ModMenus.RBMK_HEATER.get(), containerId, be, container);
        checkContainerSize(container, 1);

        addSlot(
                new Slot(container, 0, 41, 45) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof FluidIdentifierItem;
                    }
                });

        addStandardInventorySlots(playerInv, 8, 104);
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
