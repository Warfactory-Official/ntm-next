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

import com.hbm.blockentity.bomb.BombMultiBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BombMultiMenu extends BlockEntityMenu<BombMultiBlockEntity> {

    private static final int[] COLUMNS = {44, 62, 80};

    public BombMultiMenu(int containerId, Inventory playerInv, BombMultiBlockEntity bomb) {
        super(ModMenus.BOMB_MULTI.get(), containerId, bomb);
        checkContainerSize(bomb, BombMultiBlockEntity.SLOT_COUNT);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                int x = COLUMNS[col];
                int y = 26 + row * 18;
                addSlot(
                        col == 2
                                ? new SlotFiltered(
                                        container(),
                                        index,
                                        x,
                                        y,
                                        stack ->
                                                BombMultiBlockEntity.Payload.of(stack)
                                                        != BombMultiBlockEntity.Payload.NONE)
                                : new SlotFiltered(
                                        container(), index, x, y, stack -> stack.is(Items.TNT)));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index >= BombMultiBlockEntity.SLOT_COUNT) return false;
                    return moveItemStackTo(
                            stack, BombMultiBlockEntity.SLOT_COUNT, slots.size(), true);
                });
    }
}
