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
import com.hbm.blockentity.machine.oil.SolidifierBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SolidifierMenu extends BlockEntityMenu<SolidifierBlockEntity> {

    public SolidifierMenu(int containerId, Inventory playerInv, SolidifierBlockEntity be) {
        super(ModMenus.MACHINE_SOLIDIFIER.get(), containerId, be);
        checkContainerSize(be, SolidifierBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, SolidifierBlockEntity.SLOT_OUTPUT, 71, 45));
        addSlot(
                new SlotFiltered(
                        be, SolidifierBlockEntity.SLOT_BATTERY, 134, 72, EnergyItem::matches));
        addSlot(new SlotUpgrade(be, SolidifierBlockEntity.SLOT_UPGRADE_START, 98, 36));
        addSlot(new SlotUpgrade(be, SolidifierBlockEntity.SLOT_UPGRADE_END, 98, 54));
        addSlot(
                new SlotFiltered(
                        be,
                        SolidifierBlockEntity.SLOT_FLUID_ID,
                        71,
                        72,
                        s -> s.getItem() instanceof FluidIdentifierItem));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = SolidifierBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                SolidifierBlockEntity.SLOT_FLUID_ID,
                                SolidifierBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                SolidifierBlockEntity.SLOT_BATTERY,
                                SolidifierBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                SolidifierBlockEntity.SLOT_UPGRADE_START,
                                SolidifierBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    return false;
                });
    }
}
