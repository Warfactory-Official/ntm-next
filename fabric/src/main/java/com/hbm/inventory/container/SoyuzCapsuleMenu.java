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

import com.hbm.blockentity.machine.storage.CargoCapsuleBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SoyuzCapsuleMenu extends BlockEntityMenu<CargoCapsuleBlockEntity> {

    public SoyuzCapsuleMenu(int containerId, Inventory playerInv, CargoCapsuleBlockEntity capsule) {
        super(ModMenus.SOYUZ_CAPSULE.get(), containerId, capsule);
        checkContainerSize(capsule, CargoCapsuleBlockEntity.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new Slot(container(), col + row * 6, 62 + col * 18, 18 + row * 18));
            }
        }
        addSlot(new Slot(container(), CargoCapsuleBlockEntity.SLOT_ROCKET, 17, 36));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 162));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int capsule = CargoCapsuleBlockEntity.SLOT_COUNT;
                    if (index < capsule) return moveItemStackTo(stack, capsule, slots.size(), true);
                    return moveItemStackTo(stack, 0, capsule, false);
                });
    }
}
