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

import com.hbm.blockentity.machine.oil.CokerBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CokerMenu extends BlockEntityMenu<CokerBlockEntity> {

    public CokerMenu(int containerId, Inventory playerInv, CokerBlockEntity be) {
        super(ModMenus.MACHINE_COKER.get(), containerId, be);
        checkContainerSize(be, CokerBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        CokerBlockEntity.SLOT_FLUID_ID,
                        35,
                        72,
                        s -> s.getItem() instanceof FluidIdentifierItem));
        addSlot(new SlotRecipeOutput(playerInv.player, be, CokerBlockEntity.SLOT_OUTPUT, 97, 27));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = CokerBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                CokerBlockEntity.SLOT_FLUID_ID,
                                CokerBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    return false;
                });
    }
}
