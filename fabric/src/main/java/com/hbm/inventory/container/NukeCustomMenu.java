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

import com.hbm.blockentity.bomb.CustomNukeBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NukeCustomMenu extends BlockEntityMenu<CustomNukeBlockEntity> {

    private static final int PLAYER_DROP = 56;

    public NukeCustomMenu(int containerId, Inventory playerInv, CustomNukeBlockEntity bomb) {
        super(ModMenus.NUKE_CUSTOM.get(), containerId, bomb);
        checkContainerSize(bomb, CustomNukeBlockEntity.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container(), col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                        new Slot(
                                playerInv,
                                col + row * 9 + 9,
                                8 + col * 18,
                                84 + row * 18 + PLAYER_DROP));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142 + PLAYER_DROP));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int hold = CustomNukeBlockEntity.SLOT_COUNT;
                    if (index < hold) return moveItemStackTo(stack, hold, slots.size(), true);
                    return moveItemStackTo(stack, 0, hold, true);
                });
    }
}
