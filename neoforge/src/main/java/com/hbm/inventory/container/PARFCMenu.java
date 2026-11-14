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

import com.hbm.blockentity.machine.albion.RFCavityBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PARFCMenu extends BlockEntityMenu<RFCavityBlockEntity> {

    public PARFCMenu(int containerId, Inventory playerInv, RFCavityBlockEntity be) {
        super(ModMenus.PA_RFC.get(), containerId, be);
        checkContainerSize(be, RFCavityBlockEntity.INVENTORY_SIZE);

        addSlot(new Slot(container(), RFCavityBlockEntity.BATTERY_SLOT, 53, 72));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = RFCavityBlockEntity.INVENTORY_SIZE;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    return moveItemStackTo(
                            stack,
                            RFCavityBlockEntity.BATTERY_SLOT,
                            RFCavityBlockEntity.BATTERY_SLOT + 1,
                            false);
                });
    }
}
