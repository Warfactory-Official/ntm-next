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
import com.hbm.blockentity.machine.CyclotronBlockEntity;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CyclotronMenu extends BlockEntityMenu<CyclotronBlockEntity> {

    private final SyncedData data;

    public CyclotronMenu(int containerId, Inventory playerInv, CyclotronBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(CyclotronBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private CyclotronMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            CyclotronBlockEntity be) {
        super(ModMenus.MACHINE_CYCLOTRON.get(), containerId, be, container);
        checkContainerSize(container, CyclotronBlockEntity.SLOT_COUNT);
        this.data = data;

        for (int i = 0; i < CyclotronBlockEntity.LINES; i++) {
            addSlot(
                    new Slot(
                            container,
                            CyclotronBlockEntity.SLOT_PARTICLE_START + i,
                            11,
                            18 + i * 18));
        }
        for (int i = 0; i < CyclotronBlockEntity.LINES; i++) {
            addSlot(
                    new Slot(
                            container,
                            CyclotronBlockEntity.SLOT_TARGET_START + i,
                            101,
                            18 + i * 18));
        }
        for (int i = 0; i < CyclotronBlockEntity.LINES; i++) {
            addSlot(
                    new SlotRecipeOutput(
                            playerInv.player,
                            container,
                            CyclotronBlockEntity.SLOT_OUTPUT_START + i,
                            131,
                            18 + i * 18));
        }

        addSlot(new Slot(container, CyclotronBlockEntity.SLOT_BATTERY, 168, 83));
        addSlot(new SlotUpgrade(container, CyclotronBlockEntity.SLOT_UPGRADE_START, 60, 81));
        addSlot(new SlotUpgrade(container, CyclotronBlockEntity.SLOT_UPGRADE_END, 78, 81));

        addStandardInventorySlots(playerInv, 15, 133);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public int getProgress() {
        return (int) data.get("progress");
    }

    public int getPowerScaled(int i) {
        return (int) (getPower() * i / CyclotronBlockEntity.MAX_POWER);
    }

    public int getProgressScaled(int i) {
        return getProgress() * i / CyclotronBlockEntity.DURATION;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = CyclotronBlockEntity.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                CyclotronBlockEntity.SLOT_BATTERY,
                                CyclotronBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                CyclotronBlockEntity.SLOT_UPGRADE_START,
                                CyclotronBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    if (CyclotronRecipes.isParticle(stack))
                        return moveItemStackTo(
                                stack,
                                CyclotronBlockEntity.SLOT_PARTICLE_START,
                                CyclotronBlockEntity.SLOT_TARGET_START,
                                true);
                    return moveItemStackTo(
                            stack,
                            CyclotronBlockEntity.SLOT_TARGET_START,
                            CyclotronBlockEntity.SLOT_OUTPUT_START,
                            true);
                });
    }
}
