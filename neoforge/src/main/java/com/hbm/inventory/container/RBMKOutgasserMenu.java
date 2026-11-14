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

import com.hbm.blockentity.machine.rbmk.RBMKIrradiationColumnBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RBMKOutgasserMenu extends BlockEntityMenu<RBMKIrradiationColumnBlockEntity> {

    public RBMKOutgasserMenu(
            int containerId, Inventory playerInv, RBMKIrradiationColumnBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private RBMKOutgasserMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            RBMKIrradiationColumnBlockEntity be) {
        super(ModMenus.RBMK_OUTGASSER.get(), containerId, be, container);
        checkContainerSize(container, 2);

        addSlot(new Slot(container, RBMKIrradiationColumnBlockEntity.SLOT_INPUT, 48, 45));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        RBMKIrradiationColumnBlockEntity.SLOT_OUTPUT,
                        112,
                        69));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = 2;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(stack, 0, 1, false);
                });
    }
}
