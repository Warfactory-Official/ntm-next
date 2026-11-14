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

import com.hbm.blockentity.machine.reactor.ResearchReactorBlockEntity;
import com.hbm.items.machine.PlateFuelItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ReactorResearchMenu extends BlockEntityMenu<ResearchReactorBlockEntity> {
    private static final int[][] SLOT_POSITIONS = {
        {95, 22}, {131, 22}, {77, 40}, {113, 40}, {149, 40}, {95, 58}, {131, 58}, {77, 76},
        {113, 76}, {149, 76}, {95, 94}, {131, 94}
    };

    private final SyncedData data;

    public ReactorResearchMenu(
            int containerId, Inventory playerInv, ResearchReactorBlockEntity be) {
        this(
                containerId,
                playerInv,
                be,
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(ResearchReactorBlockEntity.class)
                        : SyncedData.of(be),
                be);
    }

    private ReactorResearchMenu(
            int containerId,
            Inventory playerInv,
            Container container,
            SyncedData data,
            ResearchReactorBlockEntity be) {
        super(ModMenus.REACTOR_RESEARCH.get(), containerId, be, container);
        checkContainerSize(container, ResearchReactorBlockEntity.SLOT_COUNT);
        this.data = data;

        for (int i = 0; i < SLOT_POSITIONS.length; i++) {
            addSlot(new Slot(container, i, SLOT_POSITIONS[i][0], SLOT_POSITIONS[i][1]));
        }

        addStandardInventorySlots(playerInv, 8, 140);
        addDataSlots(data);
    }

    public int getHeat() {
        return (int) data.get("heat");
    }

    public int getTotalFlux() {
        return (int) data.get("totalFlux");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack ->
                        index < ResearchReactorBlockEntity.SLOT_COUNT
                                ? moveItemStackTo(
                                        stack,
                                        ResearchReactorBlockEntity.SLOT_COUNT,
                                        slots.size(),
                                        true)
                                : stack.getItem() instanceof PlateFuelItem
                                        && moveItemStackTo(
                                                stack,
                                                0,
                                                ResearchReactorBlockEntity.SLOT_COUNT,
                                                true));
    }
}
