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
import com.hbm.blockentity.processing.FluidMixerBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MixerMenu extends BlockEntityMenu<FluidMixerBlockEntity> {

    private final SyncedData data;

    public MixerMenu(int containerId, Inventory playerInv, FluidMixerBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(FluidMixerBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private MixerMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            FluidMixerBlockEntity be) {
        super(ModMenus.MACHINE_MIXER.get(), containerId, be, container);
        checkContainerSize(container, FluidMixerBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        FluidMixerBlockEntity.SLOT_BATTERY,
                        12,
                        72,
                        EnergyItem::matches));
        addSlot(new Slot(container, FluidMixerBlockEntity.SLOT_ITEM_INPUT, 52, 72));
        addSlot(new Slot(container, FluidMixerBlockEntity.SLOT_FLUID_ID, 126, 72));
        addSlot(new SlotUpgrade(container, FluidMixerBlockEntity.SLOT_UPGRADE_START, 148, 18));
        addSlot(new SlotUpgrade(container, FluidMixerBlockEntity.SLOT_UPGRADE_START + 1, 148, 36));

        addStandardInventorySlots(playerInv, 8, 122);
        addDataSlots(data);
    }

    public long getPower() {
        return data.get("power");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int machineEnd = FluidMixerBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (stack.getItem() instanceof FluidIdentifierItem)
                        return moveItemStackTo(
                                stack,
                                FluidMixerBlockEntity.SLOT_FLUID_ID,
                                FluidMixerBlockEntity.SLOT_FLUID_ID + 1,
                                false);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                FluidMixerBlockEntity.SLOT_BATTERY,
                                FluidMixerBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                FluidMixerBlockEntity.SLOT_UPGRADE_START,
                                FluidMixerBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            FluidMixerBlockEntity.SLOT_ITEM_INPUT,
                            FluidMixerBlockEntity.SLOT_ITEM_INPUT + 1,
                            false);
                });
    }
}
