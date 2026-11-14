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

import com.hbm.blockentity.machine.MiningLaserBlockEntity;
import com.hbm.inventory.slot.SlotUpgrade;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MiningLaserMenu extends BlockEntityMenu<MiningLaserBlockEntity> {

    private final SyncedData data;

    public MiningLaserMenu(int containerId, Inventory playerInv, MiningLaserBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(MiningLaserBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private MiningLaserMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            MiningLaserBlockEntity be) {
        super(ModMenus.MACHINE_MINING_LASER.get(), containerId, be, container);
        checkContainerSize(container, MiningLaserBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(new Slot(container, MiningLaserBlockEntity.SLOT_BATTERY, 8, 108));

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(
                        new SlotUpgrade(
                                container,
                                MiningLaserBlockEntity.SLOT_UPGRADE_START + row * 4 + col,
                                98 + col * 18,
                                18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 7; col++) {
                addSlot(
                        new Slot(
                                container,
                                MiningLaserBlockEntity.SLOT_OUTPUT_START + row * 7 + col,
                                44 + col * 18,
                                72 + row * 18));
            }
        }

        addStandardInventorySlots(playerInv, 8, 140);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    public int getPowerScaled(int i) {
        return (int) (getPower() * i / MiningLaserBlockEntity.MAX_POWER);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = MiningLaserBlockEntity.SLOT_COUNT;
                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    return moveItemStackTo(
                            stack,
                            MiningLaserBlockEntity.SLOT_BATTERY,
                            MiningLaserBlockEntity.SLOT_UPGRADE_END + 1,
                            false);
                });
    }
}
