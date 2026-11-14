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
import com.hbm.blockentity.machine.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.items.machine.BlueprintsItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlasmaForgeMenu extends BlockEntityMenu<FusionPlasmaForgeBlockEntity> {

    public PlasmaForgeMenu(int containerId, Inventory playerInv, FusionPlasmaForgeBlockEntity be) {
        super(ModMenus.MACHINE_PLASMA_FORGE.get(), containerId, be);
        checkContainerSize(be, FusionPlasmaForgeBlockEntity.SLOT_COUNT);

        addSlot(
                new SlotFiltered(
                        be,
                        FusionPlasmaForgeBlockEntity.SLOT_BATTERY,
                        152,
                        82,
                        EnergyItem::matches));
        addSlot(
                new SlotFiltered(
                        be,
                        FusionPlasmaForgeBlockEntity.SLOT_BLUEPRINT,
                        35,
                        81,
                        stack -> stack.getItem() instanceof BlueprintsItem));
        addSlot(
                new SlotFiltered(
                        be,
                        FusionPlasmaForgeBlockEntity.SLOT_BOOSTER,
                        98,
                        116,
                        stack ->
                                be.canPlaceItem(FusionPlasmaForgeBlockEntity.SLOT_BOOSTER, stack)));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                int index = FusionPlasmaForgeBlockEntity.SLOT_INPUT_START + col + row * 4;
                addSlot(
                        new SlotFiltered(
                                be,
                                index,
                                8 + col * 18,
                                18 + row * 18,
                                stack -> be.canPlaceItem(index, stack)));
            }
        }

        addSlot(
                new SlotRecipeOutput(
                        playerInv.player, be, FusionPlasmaForgeBlockEntity.SLOT_OUTPUT, 116, 36));

        addStandardInventorySlots(playerInv, 8, 162);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                true,
                stack -> {
                    int machineEnd = FusionPlasmaForgeBlockEntity.SLOT_COUNT;

                    if (index < machineEnd)
                        return moveItemStackTo(stack, machineEnd, slots.size(), true);
                    if (EnergyItem.matches(stack))
                        return moveItemStackTo(
                                stack,
                                FusionPlasmaForgeBlockEntity.SLOT_BATTERY,
                                FusionPlasmaForgeBlockEntity.SLOT_BATTERY + 1,
                                false);
                    if (stack.getItem() instanceof BlueprintsItem)
                        return moveItemStackTo(
                                stack,
                                FusionPlasmaForgeBlockEntity.SLOT_BLUEPRINT,
                                FusionPlasmaForgeBlockEntity.SLOT_BLUEPRINT + 1,
                                false);
                    return moveItemStackTo(
                            stack,
                            FusionPlasmaForgeBlockEntity.SLOT_BOOSTER,
                            FusionPlasmaForgeBlockEntity.SLOT_OUTPUT,
                            false);
                });
    }
}
