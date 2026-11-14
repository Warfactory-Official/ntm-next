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

import com.hbm.inventory.slot.SlotComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

abstract class BombAssemblyMenu extends NtmContainerMenu {

    protected BombAssemblyMenu(MenuType<?> menuType, int containerId, Container components) {
        super(menuType, containerId, components);
    }

    protected final void addComponent(int x, int y) {
        var backingSlot = slots.size();
        addSlot(new SlotComponent(container(), backingSlot, x, y));
    }

    public ItemStack part(int componentIndex) {
        return container().getItem(componentIndex);
    }

    public abstract boolean isReady();

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                false,
                stack -> {
                    var playerStart = container().getContainerSize();
                    if (slotIndex >= playerStart) {
                        return false;
                    }
                    return moveItemStackTo(stack, playerStart, slots.size(), true);
                });
    }
}
