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

import com.hbm.blockentity.machine.KeyForgeBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class KeyForgeMenu extends BlockEntityMenu<KeyForgeBlockEntity> {

    public KeyForgeMenu(
            int containerId, Inventory playerInventory, KeyForgeBlockEntity blockEntity) {
        super(ModMenus.MACHINE_KEYFORGE.get(), containerId, blockEntity);
        checkContainerSize(blockEntity, KeyForgeBlockEntity.SLOT_COUNT);

        addSlot(new Slot(blockEntity, KeyForgeBlockEntity.SLOT_SOURCE, 44, 36));
        addSlot(new Slot(blockEntity, KeyForgeBlockEntity.SLOT_COPY, 80, 36));
        addSlot(new Slot(blockEntity, KeyForgeBlockEntity.SLOT_RANDOM, 116, 36));
        addStandardInventorySlots(playerInventory, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index == KeyForgeBlockEntity.SLOT_SOURCE)
                        return moveItemStackTo(
                                stack, KeyForgeBlockEntity.SLOT_COUNT, slots.size(), true);
                    return moveItemStackTo(
                            stack,
                            KeyForgeBlockEntity.SLOT_SOURCE,
                            KeyForgeBlockEntity.SLOT_SOURCE + 1,
                            false);
                });
    }
}
