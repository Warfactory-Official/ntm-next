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
import com.hbm.blockentity.machine.RadarBlockEntity;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.ModItems;
import com.hbm.items.tool.RadarLinkerItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RadarSlotsMenu extends BlockEntityMenu<RadarBlockEntity> {

    private static final int MACHINE_SLOTS = RadarBlockEntity.SLOT_COUNT;

    public RadarSlotsMenu(int containerId, Inventory playerInv, RadarBlockEntity be) {
        super(ModMenus.MACHINE_RADAR_SLOTS.get(), containerId, be);

        for (int i = 0; i < RadarBlockEntity.SLOT_LINK_COUNT; i++) {
            addSlot(new Slot(be, i, 26 + i * 18, 17));
        }

        addSlot(
                new SlotFiltered(
                        be,
                        RadarBlockEntity.SLOT_LINKER,
                        26,
                        44,
                        stack -> stack.getItem() instanceof RadarLinkerItem));
        addSlot(
                new SlotFiltered(
                        be, RadarBlockEntity.SLOT_BATTERY, 152, 44, RadarSlotsMenu::isBattery));

        addStandardInventorySlots(playerInv, 8, 103);
    }

    private static boolean isBattery(ItemStack stack) {
        return EnergyItem.matches(stack) || stack.is(ModItems.BATTERY_CREATIVE.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity().isRadarMenuValid(player);
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
                    if (isBattery(stack)) {
                        return moveItemStackTo(
                                stack, RadarBlockEntity.SLOT_BATTERY, MACHINE_SLOTS, false);
                    }
                    return moveItemStackTo(stack, 0, RadarBlockEntity.SLOT_BATTERY, false);
                });
    }
}
