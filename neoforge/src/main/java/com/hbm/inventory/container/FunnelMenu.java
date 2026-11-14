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

import com.hbm.blockentity.machine.ItemCompressionBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FunnelMenu extends BlockEntityMenu<ItemCompressionBlockEntity> {

    public FunnelMenu(int containerId, Inventory playerInv, ItemCompressionBlockEntity funnel) {
        super(ModMenus.MACHINE_FUNNEL.get(), containerId, funnel);
        checkContainerSize(funnel, ItemCompressionBlockEntity.SLOT_COUNT);

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(funnel, i, 8 + 18 * i, 18));
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new SlotRecipeOutput(playerInv.player, funnel, i + 9, 8 + 18 * i, 54));
        }

        addStandardInventorySlots(playerInv, 8, 86);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = ItemCompressionBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, 9, false);
                });
    }
}
