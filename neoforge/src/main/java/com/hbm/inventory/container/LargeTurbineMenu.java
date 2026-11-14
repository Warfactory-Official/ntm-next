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
import com.hbm.blockentity.machine.LargeTurbineBlockEntity;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LargeTurbineMenu extends BlockEntityMenu<LargeTurbineBlockEntity> {

    public LargeTurbineMenu(int containerId, Inventory playerInv, LargeTurbineBlockEntity be) {
        super(ModMenus.MACHINE_LARGE_TURBINE.get(), containerId, be);
        checkContainerSize(be, LargeTurbineBlockEntity.SLOT_COUNT);

        addSlot(new Slot(be, LargeTurbineBlockEntity.SLOT_FLUID_ID, 8, 17));
        addSlot(
                new SlotFiltered(
                        be, LargeTurbineBlockEntity.SLOT_FLUID_ID_OUT, 8, 53, SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        be,
                        LargeTurbineBlockEntity.SLOT_CONTAINER_IN,
                        44,
                        17,
                        FluidTankNTM::isFluidContainer));
        addSlot(
                new SlotFiltered(
                        be, LargeTurbineBlockEntity.SLOT_CONTAINER_OUT, 44, 53, SlotFiltered.NONE));
        addSlot(
                new SlotFiltered(
                        be, LargeTurbineBlockEntity.SLOT_BATTERY, 98, 53, EnergyItem::matches));
        addSlot(
                new SlotFiltered(
                        be,
                        LargeTurbineBlockEntity.SLOT_UNLOAD_IN,
                        152,
                        17,
                        FluidTankNTM::isFluidContainer));
        addSlot(
                new SlotFiltered(
                        be, LargeTurbineBlockEntity.SLOT_UNLOAD_OUT, 152, 53, SlotFiltered.NONE));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = LargeTurbineBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                LargeTurbineBlockEntity.SLOT_FLUID_ID,
                                LargeTurbineBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                LargeTurbineBlockEntity.SLOT_BATTERY,
                                LargeTurbineBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (FluidTankNTM.isFluidContainer(stack))
                        return moveItemStackTo(
                                stack,
                                LargeTurbineBlockEntity.SLOT_CONTAINER_IN,
                                LargeTurbineBlockEntity.SLOT_CONTAINER_IN + 1,
                                false);
                    return false;
                });
    }
}
