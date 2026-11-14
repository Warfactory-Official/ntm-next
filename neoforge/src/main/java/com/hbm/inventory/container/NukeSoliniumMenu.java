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

import com.hbm.blockentity.bomb.NukeSoliniumBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

public class NukeSoliniumMenu extends BombAssemblyMenu {

    public NukeSoliniumMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(NukeSoliniumBlockEntity.SLOT_COUNT));
    }

    public NukeSoliniumMenu(
            int containerId, Inventory playerInventory, NukeSoliniumBlockEntity bomb) {
        this(containerId, playerInventory, (Container) bomb);
    }

    private NukeSoliniumMenu(int containerId, Inventory playerInventory, Container components) {
        super(ModMenus.NUKE_SOLINIUM.get(), containerId, components);

        addComponent(26, 18);
        addComponent(53, 18);
        addComponent(107, 18);
        addComponent(134, 18);
        addComponent(80, 36);
        addComponent(26, 54);
        addComponent(53, 54);
        addComponent(107, 54);
        addComponent(134, 54);

        addStandardInventorySlots(playerInventory, 8, 140);
    }

    @Override
    public boolean isReady() {
        return NukeSoliniumBlockEntity.isReady(container());
    }
}
