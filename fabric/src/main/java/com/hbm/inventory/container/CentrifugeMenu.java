/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.inventory.container;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.blockentity.processing.CentrifugeBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CentrifugeMenu extends NtmContainerMenu implements UpgradeTooltipMenu {

    private final SyncedData data;

    public CentrifugeMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(CentrifugeBlockEntity.SLOT_COUNT),
                SyncedData.client(CentrifugeBlockEntity.class));
    }

    public CentrifugeMenu(
            int containerId, Inventory playerInventory, CentrifugeBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, SyncedData.of(blockEntity));
    }

    private CentrifugeMenu(
            int containerId, Inventory playerInventory, Container inventory, SyncedData data) {
        super(ModMenus.MACHINE_CENTRIFUGE.get(), containerId, inventory);
        checkContainerSize(inventory, CentrifugeBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(inventory, CentrifugeBlockEntity.SLOT_INPUT, 44, 57));
        addSlot(new Slot(inventory, CentrifugeBlockEntity.SLOT_BATTERY, 8, 57));
        for (var column = 0; column < 4; column++) {
            int index = CentrifugeBlockEntity.SLOT_OUTPUT_START + column;
            int x = 70 + column * 20;
            addSlot(new SlotRecipeOutput(playerInventory.player, inventory, index, x, 57));
        }
        for (var row = 0; row < 2; row++) {
            int index = CentrifugeBlockEntity.SLOT_UPGRADE_START + row;
            int y = 31 + row * 18;
            addSlot(new SlotUpgrade(inventory, index, 156, y));
        }
        addStandardInventorySlots(playerInventory, 11, 107);
        addDataSlots(data);
    }

    public int getProgress() {
        return data.getInt("progress");
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public void appendUpgradeTooltip(MachineUpgradeItem upgrade, List<Component> tooltip) {
        if (UpgradeInfo.appendHeader(
                ModBlocks.MACHINE_CENTRIFUGE.get(),
                CentrifugeBlockEntity.VALID_UPGRADES,
                upgrade,
                tooltip)) {
            UpgradeInfo.centrifuge(upgrade.type, upgrade.tier, tooltip);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMove(
                player,
                slotIndex,
                true,
                stack -> {
                    if (slotIndex < CentrifugeBlockEntity.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, CentrifugeBlockEntity.SLOT_COUNT, slots.size(), true);
                    }
                    return moveFromPlayer(stack);
                });
    }

    private boolean moveFromPlayer(ItemStack stack) {
        if (EnergyItem.matches(stack)) {
            return moveItemStackTo(
                    stack,
                    CentrifugeBlockEntity.SLOT_BATTERY,
                    CentrifugeBlockEntity.SLOT_BATTERY + 1,
                    false);
        }
        if (MachineUpgradeItem.isUpgrade(stack)) {
            return moveItemStackTo(
                    stack,
                    CentrifugeBlockEntity.SLOT_UPGRADE_START,
                    CentrifugeBlockEntity.SLOT_UPGRADE_END + 1,
                    false);
        }
        return moveItemStackTo(
                stack,
                CentrifugeBlockEntity.SLOT_INPUT,
                CentrifugeBlockEntity.SLOT_INPUT + 1,
                false);
    }
}
