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

import com.hbm.blockentity.network.pneumatic.PneumaticTubeBlockEntity;
import com.hbm.inventory.slot.SlotPattern;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class PneumoTubeMenu extends BlockEntityMenu<PneumaticTubeBlockEntity> {
    public PneumoTubeMenu(
            int containerId, Inventory playerInventory, PneumaticTubeBlockEntity machine) {
        super(ModMenus.PNEUMATIC_TUBE.get(), containerId, machine);

        for (var index = 0; index < PneumaticTubeBlockEntity.SLOT_COUNT; index++) {
            var x = 35 + index % 5 * 18;
            var y = 17 + index / 5 * 18;
            addSlot(new SlotPattern(machine, index, x, y));
        }
        addStandardInventorySlots(playerInventory, 8, 103);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput action, Player player) {
        if (slotIndex >= 0 && slotIndex < PneumaticTubeBlockEntity.SLOT_COUNT) {
            var machine = blockEntity();
            var template = getSlot(slotIndex);
            if (action == ContainerInput.PICKUP && button == 1 && !template.getItem().isEmpty()) {
                machine.advanceFilterMode(slotIndex);
            } else {
                template.set(getCarried());
                machine.initializeFilter(template.getItem(), slotIndex);
            }
            return;
        }
        super.clicked(slotIndex, button, action, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
