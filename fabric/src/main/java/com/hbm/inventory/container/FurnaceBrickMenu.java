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

import com.hbm.blockentity.machine.thermal.BrickFurnaceBlockEntity;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotSmelting;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FurnaceBrickMenu extends BlockEntityMenu<BrickFurnaceBlockEntity> {

    public FurnaceBrickMenu(int containerId, Inventory playerInv, BrickFurnaceBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private FurnaceBrickMenu(
            int containerId, Inventory playerInv, Container container, BrickFurnaceBlockEntity be) {
        super(ModMenus.FURNACE_BRICK.get(), containerId, be, container);
        checkContainerSize(container, BrickFurnaceBlockEntity.SLOT_COUNT);

        addSlot(new Slot(container, BrickFurnaceBlockEntity.SLOT_INPUT, 62, 35));
        addSlot(new Slot(container, BrickFurnaceBlockEntity.SLOT_FUEL, 35, 17));
        addSlot(
                new SlotSmelting(
                        playerInv.player, be, BrickFurnaceBlockEntity.SLOT_OUTPUT, 116, 35));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, container, BrickFurnaceBlockEntity.SLOT_ASH, 35, 53));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = BrickFurnaceBlockEntity.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (container().canPlaceItem(BrickFurnaceBlockEntity.SLOT_FUEL, stack))
                        return moveItemStackTo(
                                        stack,
                                        BrickFurnaceBlockEntity.SLOT_FUEL,
                                        BrickFurnaceBlockEntity.SLOT_FUEL + 1,
                                        false)
                                || moveItemStackTo(
                                        stack,
                                        BrickFurnaceBlockEntity.SLOT_INPUT,
                                        BrickFurnaceBlockEntity.SLOT_INPUT + 1,
                                        false);
                    return moveItemStackTo(
                            stack,
                            BrickFurnaceBlockEntity.SLOT_INPUT,
                            BrickFurnaceBlockEntity.SLOT_INPUT + 1,
                            false);
                });
    }
}
