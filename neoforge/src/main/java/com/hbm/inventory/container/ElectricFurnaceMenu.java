/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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
import com.hbm.blockentity.machine.ElectricFurnaceBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotSmelting;
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
import org.jspecify.annotations.Nullable;

public class ElectricFurnaceMenu extends NtmContainerMenu implements UpgradeTooltipMenu {

    private final SyncedData data;

    public ElectricFurnaceMenu(int containerId, Inventory playerInv) {
        this(
                containerId,
                playerInv,
                new SimpleContainer(ElectricFurnaceBlockEntity.SLOT_COUNT),
                null,
                SyncedData.client(ElectricFurnaceBlockEntity.class));
    }

    public ElectricFurnaceMenu(
            int containerId, Inventory playerInv, ElectricFurnaceBlockEntity be) {
        this(containerId, playerInv, be, be, SyncedData.of(be));
    }

    private ElectricFurnaceMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            @Nullable ElectricFurnaceBlockEntity furnace,
            SyncedData data) {
        super(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), containerId, container);
        checkContainerSize(container, ElectricFurnaceBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        ElectricFurnaceBlockEntity.SLOT_BATTERY,
                        152,
                        54,
                        EnergyItem::matches));
        addSlot(new Slot(container, ElectricFurnaceBlockEntity.SLOT_INPUT, 20, 35));
        addSlot(
                furnace != null
                        ? new SlotSmelting(
                                playerInv.player,
                                furnace,
                                ElectricFurnaceBlockEntity.SLOT_OUTPUT,
                                80,
                                35)
                        : new SlotSmelting(
                                playerInv.player,
                                container,
                                ElectricFurnaceBlockEntity.SLOT_OUTPUT,
                                80,
                                35));
        addSlot(new SlotUpgrade(container, ElectricFurnaceBlockEntity.SLOT_UPGRADE, 111, 34));

        addStandardInventorySlots(playerInv, 8, 104);
        addDataSlots(data);
    }

    @Override
    public void appendUpgradeTooltip(MachineUpgradeItem upgrade, List<Component> tooltip) {
        if (UpgradeInfo.appendHeader(
                ModBlocks.MACHINE_ELECTRIC_FURNACE.get(),
                ElectricFurnaceBlockEntity.VALID_UPGRADES,
                upgrade,
                tooltip)) {
            UpgradeInfo.electricFurnace(upgrade.type, upgrade.tier, tooltip);
        }
    }

    public int getProgress() {
        return data.getInt("progress");
    }

    public int getMaxProgress() {
        return data.getInt("maxProgress");
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = ElectricFurnaceBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();
                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                ElectricFurnaceBlockEntity.SLOT_BATTERY,
                                ElectricFurnaceBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                ElectricFurnaceBlockEntity.SLOT_UPGRADE,
                                ElectricFurnaceBlockEntity.SLOT_UPGRADE + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            ElectricFurnaceBlockEntity.SLOT_INPUT,
                            ElectricFurnaceBlockEntity.SLOT_INPUT + 1,
                            false);
                });
    }
}
