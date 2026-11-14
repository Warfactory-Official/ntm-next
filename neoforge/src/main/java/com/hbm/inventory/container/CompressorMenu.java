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
import com.hbm.blockentity.machine.PneumaticCompressorBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CompressorMenu extends BlockEntityMenu<PneumaticCompressorBlockEntity> {

    public CompressorMenu(int containerId, Inventory playerInv, PneumaticCompressorBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private CompressorMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            PneumaticCompressorBlockEntity be) {
        super(ModMenus.MACHINE_COMPRESSOR.get(), containerId, be, container);
        checkContainerSize(container, PneumaticCompressorBlockEntity.SLOT_COUNT);

        addSlot(new Slot(container, PneumaticCompressorBlockEntity.SLOT_FLUID_ID, 17, 72));
        addSlot(
                new SlotFiltered(
                        container,
                        PneumaticCompressorBlockEntity.SLOT_BATTERY,
                        152,
                        72,
                        EnergyItem::matches));
        addSlot(
                new SlotUpgrade(
                        container, PneumaticCompressorBlockEntity.SLOT_UPGRADE_START, 52, 72));
        addSlot(
                new SlotUpgrade(
                        container, PneumaticCompressorBlockEntity.SLOT_UPGRADE_END, 70, 72));

        addStandardInventorySlots(playerInv, 8, 122);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = PneumaticCompressorBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                PneumaticCompressorBlockEntity.SLOT_FLUID_ID,
                                PneumaticCompressorBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                PneumaticCompressorBlockEntity.SLOT_BATTERY,
                                PneumaticCompressorBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                PneumaticCompressorBlockEntity.SLOT_UPGRADE_START,
                                PneumaticCompressorBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    return false;
                });
    }
}
