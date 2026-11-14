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
import com.hbm.blockentity.machine.PrecisionAssemblerBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PrecAssMenu extends BlockEntityMenu<PrecisionAssemblerBlockEntity> {

    private final SyncedData data;

    public PrecAssMenu(int containerId, Inventory playerInv, PrecisionAssemblerBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(PrecisionAssemblerBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private PrecAssMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            PrecisionAssemblerBlockEntity be) {
        super(ModMenus.MACHINE_PRECASS.get(), containerId, be, container);
        checkContainerSize(container, PrecisionAssemblerBlockEntity.SLOT_COUNT);
        this.data = data;

        addSlot(
                new SlotFiltered(
                        container,
                        PrecisionAssemblerBlockEntity.SLOT_BATTERY,
                        152,
                        81,
                        EnergyItem::matches));
        addSlot(
                new SlotFiltered(
                        container,
                        PrecisionAssemblerBlockEntity.SLOT_BLUEPRINT,
                        35,
                        126,
                        stack -> stack.getItem() instanceof BlueprintsItem));
        addSlot(
                new SlotUpgrade(
                        container, PrecisionAssemblerBlockEntity.SLOT_UPGRADE_START, 152, 108));
        addSlot(
                new SlotUpgrade(
                        container, PrecisionAssemblerBlockEntity.SLOT_UPGRADE_END, 152, 126));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(
                        SlotFiltered.gated(
                                container,
                                PrecisionAssemblerBlockEntity.SLOT_INPUT_START + row * 3 + col,
                                8 + col * 18,
                                27 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(
                        new SlotRecipeOutput(
                                playerInv.player,
                                container,
                                PrecisionAssemblerBlockEntity.SLOT_OUTPUT_START + row * 3 + col,
                                80 + col * 18,
                                27 + row * 18));
            }
        }

        addStandardInventorySlots(playerInv, 8, 174);
        addDataSlots(data);
    }

    public int getProgressScaled(int i) {
        return (int) Math.ceil(i * blockEntity().module.progress);
    }

    public long getPower() {
        return data.get("power");
    }

    public long getMaxPower() {
        return data.get("maxPower");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = PrecisionAssemblerBlockEntity.SLOT_COUNT;
                    int invEnd = slots.size();

                    if (index < machineEnd) return moveItemStackTo(stack, machineEnd, invEnd, true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                PrecisionAssemblerBlockEntity.SLOT_BATTERY,
                                PrecisionAssemblerBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof BlueprintsItem)
                        return moveItemStackTo(
                                stack,
                                PrecisionAssemblerBlockEntity.SLOT_BLUEPRINT,
                                PrecisionAssemblerBlockEntity.SLOT_BLUEPRINT + 1,
                                false);
                    if (MachineUpgradeItem.isUpgrade(stack))
                        return moveItemStackTo(
                                stack,
                                PrecisionAssemblerBlockEntity.SLOT_UPGRADE_START,
                                PrecisionAssemblerBlockEntity.SLOT_UPGRADE_END + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            PrecisionAssemblerBlockEntity.SLOT_INPUT_START,
                            PrecisionAssemblerBlockEntity.SLOT_OUTPUT_END + 1,
                            false);
                });
    }
}
