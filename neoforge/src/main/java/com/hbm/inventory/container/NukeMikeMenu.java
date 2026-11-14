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

import com.hbm.blockentity.bomb.NukeMikeBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

public class NukeMikeMenu extends BombAssemblyMenu {

    public NukeMikeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(NukeMikeBlockEntity.SLOT_COUNT));
    }

    public NukeMikeMenu(int containerId, Inventory playerInventory, NukeMikeBlockEntity bomb) {
        this(containerId, playerInventory, (Container) bomb);
    }

    private NukeMikeMenu(int containerId, Inventory playerInventory, Container components) {
        super(ModMenus.NUKE_MIKE.get(), containerId, components);

        for (var column = 0; column < 2; column++) {
            for (var row = 0; row < 2; row++) {
                var x = 26 + column * 18;
                var y = 83 + row * 18;
                addComponent(x, y);
            }
        }
        addComponent(39, 35);

        var rowStart = slots.size();
        for (var backingSlot = rowStart;
                backingSlot < NukeMikeBlockEntity.SLOT_COUNT;
                backingSlot++) {
            var x = 98 + (backingSlot - rowStart) * 18;
            addComponent(x, 91);
        }

        addStandardInventorySlots(playerInventory, 8, 135);
    }

    @Override
    public boolean isReady() {
        return NukeMikeBlockEntity.isReady(container());
    }

    public boolean isFilled() {
        return NukeMikeBlockEntity.isFilled(container());
    }
}
