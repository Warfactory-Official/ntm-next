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

import com.hbm.blockentity.machine.storage.CrateBlockEntity;
import com.hbm.blockentity.machine.storage.CrateType;
import com.hbm.platform.Platform;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CrateMenu extends NtmContainerMenu {

    private final CrateType type;
    private final boolean opensContainer;

    public CrateMenu(int containerId, Inventory playerInv, CrateType type) {
        this(containerId, playerInv, type, new SimpleContainer(type.slots));
    }

    public CrateMenu(int containerId, Inventory playerInv, CrateBlockEntity crate) {
        this(containerId, playerInv, crate.crateType, crate);
    }

    public CrateMenu(int containerId, Inventory playerInv, CrateType type, Container container) {
        this(ModMenus.crateMenuType(type).get(), containerId, playerInv, type, container);
    }

    protected CrateMenu(
            MenuType<?> menuType,
            int containerId,
            Inventory playerInv,
            CrateType type,
            Container container) {
        super(menuType, containerId, container);
        checkContainerSize(container, type.slots);
        this.type = type;

        for (int row = 0; row < type.rows; row++) {
            for (int col = 0; col < type.columns; col++) {
                int slot = col + row * type.columns;
                addSlot(
                        new ShulkerBoxSlot(
                                container, slot, type.crateX + col * 18, type.crateY + row * 18) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return Platform.canFitInsideContainerItems(stack)
                                        && super.mayPlace(stack);
                            }
                        });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                        new Slot(
                                playerInv,
                                col + row * 9 + 9,
                                type.playerInventoryX + col * 18,
                                type.playerInventoryY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, type.playerInventoryX + col * 18, type.hotbarY));
        }
        opensContainer = !playerInv.player.isSpectator();
        if (opensContainer) container.startOpen(playerInv.player);
    }

    public CrateType getCrateType() {
        return type;
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
                    int crateSlots = type.slots;
                    int invEnd = slots.size();
                    if (index < crateSlots)
                        return moveItemStackToFiltered(stack, crateSlots, invEnd, true);
                    return moveItemStackToFiltered(stack, 0, crateSlots, false);
                });
    }
}
