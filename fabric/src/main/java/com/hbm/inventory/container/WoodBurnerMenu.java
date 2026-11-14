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

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.blockentity.machine.WoodGeneratorBlockEntity;
import com.hbm.handler.FuelHandler;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WoodBurnerMenu extends BlockEntityMenu<WoodGeneratorBlockEntity> {

    public WoodBurnerMenu(int containerId, Inventory playerInv, WoodGeneratorBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private WoodBurnerMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            WoodGeneratorBlockEntity be) {
        super(ModMenus.MACHINE_WOOD_BURNER.get(), containerId, be, container);
        checkContainerSize(container, WoodGeneratorBlockEntity.SLOT_COUNT);

        addSlot(new Slot(container, WoodGeneratorBlockEntity.SLOT_FUEL, 26, 18));
        addSlot(
                new SlotFiltered(
                        container, WoodGeneratorBlockEntity.SLOT_ASH, 26, 54, SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        WoodGeneratorBlockEntity.SLOT_FLUID_ID,
                        98,
                        54,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));
        addSlot(new Slot(container, WoodGeneratorBlockEntity.SLOT_FLUID_IN, 98, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        WoodGeneratorBlockEntity.SLOT_FLUID_OUT,
                        98,
                        36,
                        SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        container,
                        WoodGeneratorBlockEntity.SLOT_BATTERY,
                        143,
                        54,
                        EnergyItem::matches));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = WoodGeneratorBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                WoodGeneratorBlockEntity.SLOT_FLUID_ID,
                                WoodGeneratorBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                WoodGeneratorBlockEntity.SLOT_BATTERY,
                                WoodGeneratorBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (FuelHandler.getBurnTime(player.level(), stack) > 0)
                        return moveItemStackTo(
                                stack,
                                WoodGeneratorBlockEntity.SLOT_FUEL,
                                WoodGeneratorBlockEntity.SLOT_FUEL + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            WoodGeneratorBlockEntity.SLOT_FLUID_IN,
                            WoodGeneratorBlockEntity.SLOT_FLUID_IN + 1,
                            false);
                });
    }
}
