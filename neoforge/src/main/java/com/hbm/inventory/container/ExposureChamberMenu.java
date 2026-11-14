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
import com.hbm.blockentity.machine.ExposureChamberBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ExposureChamberMenu extends BlockEntityMenu<ExposureChamberBlockEntity> {
    private static final int PARTICLE = 0;
    private static final int OUTPUT = 3;
    private static final int BATTERY = 4;
    private static final int UPGRADE_START = 5;
    private static final int UPGRADE_END = 6;
    private static final int MACHINE_SLOTS = 7;

    public ExposureChamberMenu(
            int containerId, Inventory playerInv, ExposureChamberBlockEntity be) {
        this(containerId, playerInv, be, be);
    }

    private ExposureChamberMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            ExposureChamberBlockEntity be) {
        super(ModMenus.MACHINE_EXPOSURE_CHAMBER.get(), containerId, be, container);
        checkContainerSize(container, ExposureChamberBlockEntity.INVENTORY_SIZE);

        addSlot(new Slot(container, ExposureChamberBlockEntity.CAPSULE_SLOT, 8, 18));
        addSlot(
                new SlotFiltered(
                        container,
                        ExposureChamberBlockEntity.CONTAINER_SLOT,
                        8,
                        54,
                        SlotFiltered.NONE));
        addSlot(new Slot(container, ExposureChamberBlockEntity.MATERIAL_SLOT, 80, 36));
        addSlot(
                new SlotRecipeOutput(
                        playerInv.player,
                        container,
                        ExposureChamberBlockEntity.PRODUCT_SLOT,
                        116,
                        36));
        addSlot(new Slot(container, ExposureChamberBlockEntity.BATTERY_SLOT, 152, 54));
        addSlot(new Slot(container, ExposureChamberBlockEntity.FIRST_UPGRADE_SLOT, 44, 54));
        addSlot(new Slot(container, ExposureChamberBlockEntity.LAST_UPGRADE_SLOT, 62, 54));

        addStandardInventorySlots(playerInv, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    if (index < MACHINE_SLOTS)
                        return moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true);
                    if (MachineUpgradeItem.isUpgrade(stack)) {
                        return moveItemStackTo(stack, UPGRADE_START, UPGRADE_END + 1, false);
                    }
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(stack, BATTERY, BATTERY + 1, false);
                    return moveItemStackTo(stack, PARTICLE, OUTPUT, false);
                });
    }
}
