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

import com.hbm.blockentity.machine.HeaterOilburnerBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OilburnerMenu extends BlockEntityMenu<HeaterOilburnerBlockEntity> {

    public OilburnerMenu(int containerId, Inventory playerInv, HeaterOilburnerBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private OilburnerMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            HeaterOilburnerBlockEntity be) {
        super(ModMenus.HEATER_OILBURNER.get(), containerId, be, container);
        checkContainerSize(container, 3);
        container.startOpen(playerInv.player);

        addSlot(new Slot(container, HeaterOilburnerBlockEntity.SLOT_FLUID_IN, 26, 17));
        addSlot(
                new SlotFiltered(
                        container,
                        HeaterOilburnerBlockEntity.SLOT_FLUID_OUT,
                        26,
                        53,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        HeaterOilburnerBlockEntity.SLOT_FLUID_ID,
                        44,
                        71,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 121);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = 3;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                HeaterOilburnerBlockEntity.SLOT_FLUID_ID,
                                HeaterOilburnerBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            HeaterOilburnerBlockEntity.SLOT_FLUID_IN,
                            HeaterOilburnerBlockEntity.SLOT_FLUID_IN + 1,
                            false);
                });
    }
}
