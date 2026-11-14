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

import com.hbm.blockentity.network.CraneUnboxerBlockEntity;
import com.hbm.inventory.slot.SlotUpgrade;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CraneUnboxerMenu extends BlockEntityMenu<CraneUnboxerBlockEntity> {

    public CraneUnboxerMenu(
            int containerId, Inventory playerInventory, CraneUnboxerBlockEntity be) {
        super(ModMenus.CRANE_UNBOXER.get(), containerId, be);
        checkContainerSize(be, CraneUnboxerBlockEntity.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                addSlot(new Slot(be, j + i * 7, 8 + j * 18, 17 + i * 18));
            }
        }

        addSlot(new SlotUpgrade(be, CraneUnboxerBlockEntity.SLOT_UPGRADE_STACK, 152, 23));
        addSlot(new SlotUpgrade(be, CraneUnboxerBlockEntity.SLOT_UPGRADE_EJECTOR, 152, 47));

        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, CraneUnboxerBlockEntity.SLOT_COUNT);
    }
}
