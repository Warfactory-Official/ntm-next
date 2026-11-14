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
import com.hbm.blockentity.machine.CombustionEngineBlockEntity;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.PistonsItem;
import com.hbm.items.machine.FluidIdentifierItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CombustionEngineMenu extends BlockEntityMenu<CombustionEngineBlockEntity> {

    private final CombustionEngineBlockEntity engine;
    private final boolean opensContainer;

    public CombustionEngineMenu(
            int containerId, Inventory playerInv, CombustionEngineBlockEntity be) {
        super(ModMenus.MACHINE_COMBUSTION_ENGINE.get(), containerId, be);
        checkContainerSize(be, CombustionEngineBlockEntity.SLOT_COUNT);
        this.engine = be;
        opensContainer = !playerInv.player.isSpectator();
        if (opensContainer) be.openInventory();

        addSlot(new Slot(be, CombustionEngineBlockEntity.SLOT_FLUID_IN, 17, 17));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        be,
                        CombustionEngineBlockEntity.SLOT_CONTAINER_OUT,
                        17,
                        53));
        addSlot(
                new SlotFiltered(
                        be,
                        CombustionEngineBlockEntity.SLOT_PISTON,
                        88,
                        71,
                        stack -> stack.getItem() instanceof PistonsItem));
        addSlot(
                new SlotFiltered(
                        be,
                        CombustionEngineBlockEntity.SLOT_BATTERY,
                        143,
                        71,
                        EnergyItem::matches));
        addSlot(
                new SlotFiltered(
                        be,
                        CombustionEngineBlockEntity.SLOT_FLUID_ID,
                        35,
                        71,
                        stack -> stack.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 121);
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return FluidTankNTM.isFluidContainer(stack);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (opensContainer) engine.closeInventory();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = CombustionEngineBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                CombustionEngineBlockEntity.SLOT_FLUID_ID,
                                CombustionEngineBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                CombustionEngineBlockEntity.SLOT_BATTERY,
                                CombustionEngineBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof PistonsItem)
                        return moveItemStackTo(
                                stack,
                                CombustionEngineBlockEntity.SLOT_PISTON,
                                CombustionEngineBlockEntity.SLOT_PISTON + 1,
                                false);
                    if (isFluidContainer(stack))
                        return moveItemStackTo(
                                stack,
                                CombustionEngineBlockEntity.SLOT_FLUID_IN,
                                CombustionEngineBlockEntity.SLOT_FLUID_IN + 1,
                                false);
                    return false;
                });
    }
}
