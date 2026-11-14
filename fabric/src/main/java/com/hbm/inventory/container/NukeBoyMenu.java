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

import com.hbm.blockentity.bomb.NukeBoyBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

public class NukeBoyMenu extends BombAssemblyMenu {

    public NukeBoyMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(NukeBoyBlockEntity.SLOT_COUNT));
    }

    public NukeBoyMenu(int containerId, Inventory playerInventory, NukeBoyBlockEntity bomb) {
        this(containerId, playerInventory, (Container) bomb);
    }

    private NukeBoyMenu(int containerId, Inventory playerInventory, Container components) {
        super(ModMenus.NUKE_BOY.get(), containerId, components);

        for (var backingSlot = 0; backingSlot < NukeBoyBlockEntity.SLOT_COUNT; backingSlot++) {
            var x = 26 + backingSlot * 18;
            addComponent(x, 36);
        }

        addStandardInventorySlots(playerInventory, 8, 140);
    }

    @Override
    public boolean isReady() {
        return NukeBoyBlockEntity.isReady(container());
    }
}
