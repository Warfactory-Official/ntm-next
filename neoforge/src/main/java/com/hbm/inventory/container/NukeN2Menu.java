/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.container;

import com.hbm.blockentity.bomb.NukeN2BlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

public class NukeN2Menu extends BombAssemblyMenu {

    public NukeN2Menu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(NukeN2BlockEntity.SLOT_COUNT));
    }

    public NukeN2Menu(int containerId, Inventory playerInventory, NukeN2BlockEntity bomb) {
        this(containerId, playerInventory, (Container) bomb);
    }

    private NukeN2Menu(int containerId, Inventory playerInventory, Container components) {
        super(ModMenus.NUKE_N2.get(), containerId, components);

        for (var backingSlot = 0; backingSlot < NukeN2BlockEntity.SLOT_COUNT; backingSlot++) {
            var column = backingSlot % 3;
            var row = backingSlot / 3;
            var x = 98 + column * 18;
            var y = 36 + row * 18;
            addComponent(x, y);
        }

        addStandardInventorySlots(playerInventory, 8, 140);
    }

    @Override
    public boolean isReady() {
        return NukeN2BlockEntity.isReady(container());
    }
}
