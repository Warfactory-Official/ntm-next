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

import com.hbm.blockentity.bomb.NukeFleijaBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

public class NukeFleijaMenu extends BombAssemblyMenu {

    public NukeFleijaMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(NukeFleijaBlockEntity.SLOT_COUNT));
    }

    public NukeFleijaMenu(int containerId, Inventory playerInventory, NukeFleijaBlockEntity bomb) {
        this(containerId, playerInventory, (Container) bomb);
    }

    private NukeFleijaMenu(int containerId, Inventory playerInventory, Container components) {
        super(ModMenus.NUKE_FLEIJA.get(), containerId, components);

        addComponent(8, 36);
        addComponent(152, 36);
        for (var row = 0; row < 3; row++) {
            var y = 18 + row * 18;
            addComponent(44, y);
        }

        var gridStart = slots.size();
        for (var backingSlot = gridStart;
                backingSlot < NukeFleijaBlockEntity.SLOT_COUNT;
                backingSlot++) {
            var gridIndex = backingSlot - gridStart;
            var column = gridIndex % 2;
            var row = gridIndex / 2;
            var x = 80 + column * 18;
            var y = 18 + row * 18;
            addComponent(x, y);
        }

        addStandardInventorySlots(playerInventory, 8, 140);
    }

    @Override
    public boolean isReady() {
        return NukeFleijaBlockEntity.isReady(container());
    }
}
