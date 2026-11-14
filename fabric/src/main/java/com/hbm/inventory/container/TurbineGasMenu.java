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
import com.hbm.blockentity.machine.TurbineGasBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public class TurbineGasMenu extends BlockEntityMenu<TurbineGasBlockEntity> {

    public TurbineGasMenu(int containerId, Inventory playerInv, TurbineGasBlockEntity be) {
        super(ModMenus.MACHINE_TURBINEGAS.get(), containerId, be);
        checkContainerSize(be, TurbineGasBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be, TurbineGasBlockEntity.SLOT_BATTERY, 8, 109, EnergyItem::matches));
        addSlot(
                new SlotFiltered(
                        be,
                        TurbineGasBlockEntity.SLOT_FLUID_ID,
                        36,
                        17,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 141);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = TurbineGasBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem) {
                        Fluid type =
                                stack.getOrDefault(
                                                ModDataComponents.FLUID_IDENTIFIER.get(),
                                                FluidIdentifierData.EMPTY)
                                        .primary();
                        return TurbineGasBlockEntity.isGasFuel(type)
                                && moveItemStackTo(
                                        stack,
                                        TurbineGasBlockEntity.SLOT_FLUID_ID,
                                        TurbineGasBlockEntity.SLOT_FLUID_ID + 1,
                                        false);
                    }
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                TurbineGasBlockEntity.SLOT_BATTERY,
                                TurbineGasBlockEntity.SLOT_BATTERY + 1,
                                false);
                    return false;
                });
    }
}
