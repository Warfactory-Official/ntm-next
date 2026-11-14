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

import com.hbm.blockentity.machine.dfc.DfcStabilizerBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CoreStabilizerMenu extends BlockEntityMenu<DfcStabilizerBlockEntity> {

    public CoreStabilizerMenu(int containerId, Inventory playerInv, DfcStabilizerBlockEntity be) {
        super(ModMenus.CORE_STABILIZER.get(), containerId, be);
        checkContainerSize(be, DfcStabilizerBlockEntity.SLOT_COUNT);

        addSlot(new Slot(be, DfcStabilizerBlockEntity.SLOT_LENS, 80, 17));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, DfcStabilizerBlockEntity.SLOT_COUNT);
    }
}
